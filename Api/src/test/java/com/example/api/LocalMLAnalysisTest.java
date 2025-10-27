package com.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas con Machine Learning LOCAL (sin APIs externas)
 * Usa Apache Commons Math para análisis estadístico y ML
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ApiApplication.class, TestSecurityConfig.class}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalMLAnalysisTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String testToken;
    private static final String TEST_EMAIL = "ml.local@example.com";
    private static List<Map<String, Object>> testDataHistory = new ArrayList<>();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    // =====================================================
    // ML HELPER: Análisis de Clustering
    // =====================================================

    /**
     * Agrupa respuestas usando K-Means para detectar patrones
     */
    private ClusterAnalysisResult performKMeansClustering(List<double[]> dataPoints, int k) {
        try {
            List<DoublePoint> points = dataPoints.stream()
                .map(DoublePoint::new)
                .collect(Collectors.toList());

            KMeansPlusPlusClusterer<DoublePoint> clusterer = 
                new KMeansPlusPlusClusterer<>(k, 100);
            
            List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

            // Analizar distribución
            Map<Integer, Integer> distribution = new HashMap<>();
            for (int i = 0; i < clusters.size(); i++) {
                distribution.put(i, clusters.get(i).getPoints().size());
            }

            boolean isBalanced = distribution.values().stream()
                .allMatch(count -> count >= points.size() / (k * 2));

            return new ClusterAnalysisResult(clusters.size(), distribution, isBalanced);
            
        } catch (Exception e) {
            System.err.println("Clustering failed: " + e.getMessage());
            return new ClusterAnalysisResult(0, new HashMap<>(), false);
        }
    }

    /**
     * Calcula correlación entre variables usando Pearson
     */
    private double calculateCorrelation(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) {
            return 0.0;
        }
        
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        return correlation.correlation(x, y);
    }

    /**
     * Análisis de outliers usando IQR (Interquartile Range)
     */
    private OutlierAnalysisResult detectOutliersIQR(List<Double> values) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);

        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        List<Integer> outlierIndices = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            if (value < lowerBound || value > upperBound) {
                outlierIndices.add(i);
            }
        }

        return new OutlierAnalysisResult(
            outlierIndices,
            lowerBound,
            upperBound,
            !outlierIndices.isEmpty()
        );
    }

    /**
     * Análisis de tendencia usando regresión simple
     */
    private TrendAnalysisResult analyzeTrend(List<Double> values) {
        if (values.size() < 3) {
            return new TrendAnalysisResult("INSUFFICIENT_DATA", 0.0, 0.0);
        }

        // Calcular pendiente simple
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = values.size();

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        String trend;
        if (slope > values.get(0) * 0.05) trend = "STRONGLY_INCREASING";
        else if (slope > 0) trend = "INCREASING";
        else if (slope > -values.get(0) * 0.05) trend = "STABLE";
        else trend = "DECREASING";

        return new TrendAnalysisResult(trend, slope, intercept);
    }

    /**
     * Análisis de distribución normal usando Kolmogorov-Smirnov
     */
    private boolean isNormallyDistributed(List<Double> values) {
        if (values.size() < 10) return false;

        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);

        // Test simple de normalidad usando skewness y kurtosis
        double skewness = stats.getSkewness();
        double kurtosis = stats.getKurtosis();

        // Distribución normal: skewness ≈ 0, kurtosis ≈ 3
        return Math.abs(skewness) < 1.0 && Math.abs(kurtosis - 3.0) < 2.0;
    }

    // =====================================================
    // PRUEBAS CON ML LOCAL
    // =====================================================

    @Test
    @Order(1)
    @DisplayName("ML01 - Registro y análisis estadístico de respuesta")
    void testRegistroConAnalisisEstadistico() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", TEST_EMAIL);
        credentials.put("password", "MLTest123!");

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/registro",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        // 📊 ANÁLISIS ML: Evaluar estructura de respuesta
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        // Analizar completitud de campos
        List<String> expectedFields = Arrays.asList("mensaje", "id");
        long completeness = expectedFields.stream()
            .filter(body::containsKey)
            .count();
        
        double completenessScore = (completeness / (double) expectedFields.size()) * 100;
        System.out.println("📊 ML Completeness Score: " + completenessScore + "%");
        
        assertTrue(completenessScore >= 80, "Respuesta incompleta según análisis ML");

        // Guardar para análisis histórico
        testDataHistory.add(body);
    }

    @Test
    @Order(2)
    @DisplayName("ML02 - Login y clustering de tiempos de respuesta")
    void testLoginConClusteringTiempos() {
        List<Long> responseTimes = new ArrayList<>();
        List<Integer> statusCodes = new ArrayList<>();

        // Realizar múltiples requests para análisis
        for (int i = 0; i < 10; i++) {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("email", TEST_EMAIL);
            credentials.put("password", "MLTest123!");

            long startTime = System.currentTimeMillis();
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/login",
                credentials,
                Map.class
            );
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            responseTimes.add(responseTime);
            statusCodes.add(response.getStatusCode().value());

            if (i == 0) {
                testToken = (String) response.getBody().get("token");
            }
        }

        // 🤖 ANÁLISIS ML: Clustering de tiempos de respuesta
        List<double[]> dataPoints = new ArrayList<>();
        for (int i = 0; i < responseTimes.size(); i++) {
            dataPoints.add(new double[]{
                responseTimes.get(i).doubleValue(),
                statusCodes.get(i).doubleValue()
            });
        }

        ClusterAnalysisResult clusterResult = performKMeansClustering(dataPoints, 2);
        System.out.println("🔍 ML Clustering: " + clusterResult.clusterCount + " clusters detectados");
        System.out.println("📊 Distribución: " + clusterResult.distribution);
        System.out.println("⚖️ Balance: " + (clusterResult.isBalanced ? "✅ Balanceado" : "⚠️ Desbalanceado"));

        // Análisis estadístico de tiempos
        DescriptiveStatistics stats = new DescriptiveStatistics();
        responseTimes.forEach(time -> stats.addValue(time.doubleValue()));

        double avgTime = stats.getMean();
        double maxTime = stats.getMax();
        double stdDev = stats.getStandardDeviation();

        System.out.println("⏱️ Tiempo promedio: " + avgTime + "ms");
        System.out.println("⏱️ Tiempo máximo: " + maxTime + "ms");
        System.out.println("📊 Desviación estándar: " + stdDev + "ms");

        // Validaciones con ML
        assertTrue(avgTime < 5000, "Tiempo promedio muy alto: " + avgTime + "ms");
        assertTrue(stdDev < avgTime * 0.5, "Alta variabilidad en tiempos de respuesta");
        
        // Detectar outliers en tiempos
        List<Double> times = responseTimes.stream()
            .map(Long::doubleValue)
            .collect(Collectors.toList());
        
        OutlierAnalysisResult outliers = detectOutliersIQR(times);
        System.out.println("🎯 Outliers detectados: " + outliers.outlierIndices.size());
        
        assertTrue(outliers.outlierIndices.size() <= 2, 
            "Demasiados outliers en tiempos de respuesta: " + outliers.outlierIndices);
    }

    @Test
    @Order(3)
    @DisplayName("ML03 - Análisis de correlación entre ingreso y gastos")
    void testCorrelacionIngresoGastos() {
        List<Double> ingresos = new ArrayList<>();
        List<Double> gastosTotal = new ArrayList<>();

        // Generar múltiples escenarios financieros
        for (int i = 1; i <= 10; i++) {
            double ingreso = 2000000.0 + (i * 500000.0);
            
            Map<String, Object> datos = new HashMap<>();
            datos.put("ingreso", ingreso);
            datos.put("arriendoHipo", ingreso * 0.3);
            datos.put("services", 200000.0);
            datos.put("alimentacion", 500000.0 + (i * 50000.0));
            datos.put("transporte", 300000.0);
            datos.put("otros", 150000.0);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", testToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/info_financiera",
                request,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
            
            ingresos.add(ingreso);
            gastosTotal.add(((Number) gastos.get("totalGastos")).doubleValue());
        }

        // 🤖 ANÁLISIS ML: Calcular correlación
        double[] ingresosArray = ingresos.stream().mapToDouble(Double::doubleValue).toArray();
        double[] gastosArray = gastosTotal.stream().mapToDouble(Double::doubleValue).toArray();

        double correlation = calculateCorrelation(ingresosArray, gastosArray);
        
        System.out.println("📈 Correlación Ingreso-Gastos: " + String.format("%.4f", correlation));
        
        if (correlation > 0.7) {
            System.out.println("✅ Correlación positiva fuerte detectada");
        } else if (correlation > 0.3) {
            System.out.println("⚠️ Correlación positiva moderada");
        } else {
            System.out.println("❌ Correlación débil o inexistente");
        }

        // Validación: Debe haber correlación positiva
        assertTrue(correlation > 0.5, 
            "Se esperaba correlación positiva entre ingresos y gastos, obtenido: " + correlation);

        // Análisis de tendencia
        TrendAnalysisResult trendIngresos = analyzeTrend(ingresos);
        TrendAnalysisResult trendGastos = analyzeTrend(gastosTotal);

        System.out.println("📊 Tendencia Ingresos: " + trendIngresos.trend);
        System.out.println("📊 Tendencia Gastos: " + trendGastos.trend);

        assertEquals(trendIngresos.trend, trendGastos.trend, 
            "Las tendencias deberían ser consistentes");
    }

    @Test
    @Order(4)
    @DisplayName("ML04 - Detección de anomalías en gastos con IQR")
    void testDeteccionAnomaliasConIQR() {
        List<Double> gastosMensuales = new ArrayList<>();

        // Gastos normales + 2 anomalías
        double[] gastosData = {
            1200000, 1150000, 1300000, 1250000, 1180000,  // Normal
            5000000,  // ANOMALÍA 1
            1220000, 1280000, 
            6000000,  // ANOMALÍA 2
            1190000
        };

        for (double gasto : gastosData) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("ingreso", 5000000.0);
            datos.put("arriendoHipo", gasto * 0.5);
            datos.put("services", gasto * 0.15);
            datos.put("alimentacion", gasto * 0.2);
            datos.put("transporte", gasto * 0.1);
            datos.put("otros", gasto * 0.05);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", testToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/info_financiera",
                request,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
            gastosMensuales.add(((Number) gastos.get("totalGastos")).doubleValue());
        }

        // 🤖 ANÁLISIS ML: Detección de outliers con IQR
        OutlierAnalysisResult outlierResult = detectOutliersIQR(gastosMensuales);

        System.out.println("🔍 Outliers detectados: " + outlierResult.outlierIndices.size());
        System.out.println("📊 Índices de anomalías: " + outlierResult.outlierIndices);
        System.out.println("📉 Límite inferior: " + String.format("%.2f", outlierResult.lowerBound));
        System.out.println("📈 Límite superior: " + String.format("%.2f", outlierResult.upperBound));

        // Validaciones
        assertTrue(outlierResult.hasOutliers, "No se detectaron las anomalías esperadas");
        assertTrue(outlierResult.outlierIndices.size() >= 2, 
            "Se esperaban al menos 2 anomalías, detectadas: " + outlierResult.outlierIndices.size());
        
        // Verificar que las anomalías conocidas fueron detectadas
        assertTrue(outlierResult.outlierIndices.contains(5), "Anomalía en índice 5 no detectada");
        assertTrue(outlierResult.outlierIndices.contains(8), "Anomalía en índice 8 no detectada");
    }

    @Test
    @Order(5)
    @DisplayName("ML05 - Análisis de distribución normal en datos financieros")
    void testDistribucionNormalDatosFinancieros() {
        List<Double> ingresos = new ArrayList<>();
        List<Double> disponibles = new ArrayList<>();

        // Generar datos con distribución cercana a normal
        Random random = new Random(42); // Seed para reproducibilidad
        
        for (int i = 0; i < 30; i++) {
            // Ingresos con distribución normal (media=4M, stddev=500k)
            double ingreso = 4000000.0 + (random.nextGaussian() * 500000.0);
            ingreso = Math.max(2000000, ingreso); // Evitar negativos

            Map<String, Object> datos = new HashMap<>();
            datos.put("ingreso", ingreso);
            datos.put("arriendoHipo", 1200000.0);
            datos.put("services", 250000.0);
            datos.put("alimentacion", 600000.0);
            datos.put("transporte", 350000.0);
            datos.put("otros", 200000.0);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", testToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/info_financiera",
                request,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
            
            ingresos.add(ingreso);
            disponibles.add(((Number) gastos.get("disponible")).doubleValue());
        }

        // 🤖 ANÁLISIS ML: Test de normalidad
        boolean ingresosNormales = isNormallyDistributed(ingresos);
        boolean disponiblesNormales = isNormallyDistributed(disponibles);

        System.out.println("📊 Distribución de ingresos: " + 
            (ingresosNormales ? "✅ Normal" : "⚠️ No normal"));
        System.out.println("📊 Distribución de disponibles: " + 
            (disponiblesNormales ? "✅ Normal" : "⚠️ No normal"));

        // Estadísticas descriptivas
        DescriptiveStatistics statsIngresos = new DescriptiveStatistics();
        DescriptiveStatistics statsDisponibles = new DescriptiveStatistics();
        
        ingresos.forEach(statsIngresos::addValue);
        disponibles.forEach(statsDisponibles::addValue);

        System.out.println("📈 Ingresos - Media: " + String.format("%.2f", statsIngresos.getMean()));
        System.out.println("📈 Ingresos - Skewness: " + String.format("%.4f", statsIngresos.getSkewness()));
        System.out.println("📈 Ingresos - Kurtosis: " + String.format("%.4f", statsIngresos.getKurtosis()));

        // Los ingresos deberían seguir distribución aproximadamente normal
        assertTrue(ingresosNormales, "Los ingresos no siguen distribución normal");
        
        // Skewness debe estar cerca de 0 para distribución normal
        assertTrue(Math.abs(statsIngresos.getSkewness()) < 1.5, 
            "Skewness muy alto: " + statsIngresos.getSkewness());
    }

    @Test
    @Order(6)
    @DisplayName("ML06 - Predicción de tendencias con regresión lineal")
    void testPrediccionTendenciasRegresion() {
        List<Double> gastosHistoricos = new ArrayList<>();

        // Simular gastos crecientes en el tiempo
        for (int mes = 1; mes <= 12; mes++) {
            double gastoBase = 2000000.0;
            double incremento = mes * 50000.0; // Incremento mensual
            double gasto = gastoBase + incremento;

            Map<String, Object> datos = new HashMap<>();
            datos.put("ingreso", 5000000.0);
            datos.put("arriendoHipo", gasto * 0.4);
            datos.put("services", gasto * 0.15);
            datos.put("alimentacion", gasto * 0.25);
            datos.put("transporte", gasto * 0.15);
            datos.put("otros", gasto * 0.05);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", testToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/info_financiera",
                request,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
            gastosHistoricos.add(((Number) gastos.get("totalGastos")).doubleValue());
        }

        // 🤖 ANÁLISIS ML: Detectar tendencia y predecir
        TrendAnalysisResult trend = analyzeTrend(gastosHistoricos);

        System.out.println("📈 Tendencia detectada: " + trend.trend);
        System.out.println("📊 Pendiente: " + String.format("%.2f", trend.slope));
        System.out.println("📊 Intercepto: " + String.format("%.2f", trend.intercept));

        // Predecir próximos 3 meses
        int nextMonth = gastosHistoricos.size() + 1;
        double prediccion1 = trend.intercept + (trend.slope * nextMonth);
        double prediccion2 = trend.intercept + (trend.slope * (nextMonth + 1));
        double prediccion3 = trend.intercept + (trend.slope * (nextMonth + 2));

        System.out.println("🔮 Predicción mes 13: " + String.format("%.2f", prediccion1));
        System.out.println("🔮 Predicción mes 14: " + String.format("%.2f", prediccion2));
        System.out.println("🔮 Predicción mes 15: " + String.format("%.2f", prediccion3));

        // Validaciones
        assertEquals("STRONGLY_INCREASING", trend.trend, 
            "Debería detectar tendencia creciente fuerte");
        assertTrue(trend.slope > 0, "La pendiente debe ser positiva");
        assertTrue(prediccion1 > gastosHistoricos.get(gastosHistoricos.size() - 1),
            "La predicción debe ser mayor que el último valor");
        
        // Validar que la predicción es razonable (no muy alejada)
        double lastValue = gastosHistoricos.get(gastosHistoricos.size() - 1);
        double predictedChange = ((prediccion1 - lastValue) / lastValue) * 100;
        assertTrue(predictedChange < 10, 
            "Cambio predicho muy drástico: " + predictedChange + "%");
    }

    @Test
    @Order(7)
    @DisplayName("ML07 - Clustering multidimensional de patrones financieros")
    void testClusteringMultidimensional() {
        List<double[]> patronesFinancieros = new ArrayList<>();

        // Generar 3 tipos de patrones financieros distintos
        // Patrón 1: Gastos bajos (5 casos)
        for (int i = 0; i < 5; i++) {
            patronesFinancieros.add(new double[]{
                3000000.0,  // ingreso
                1000000.0,  // gastos totales
                2000000.0   // disponible
            });
        }

        // Patrón 2: Gastos medios (5 casos)
        for (int i = 0; i < 5; i++) {
            patronesFinancieros.add(new double[]{
                5000000.0,  // ingreso
                3500000.0,  // gastos totales
                1500000.0   // disponible
            });
        }

        // Patrón 3: Gastos altos (5 casos)
        for (int i = 0; i < 5; i++) {
            patronesFinancieros.add(new double[]{
                4000000.0,  // ingreso
                3800000.0,  // gastos totales
                200000.0    // disponible
            });
        }

        // 🤖 ANÁLISIS ML: K-Means con k=3
        ClusterAnalysisResult clusterResult = performKMeansClustering(patronesFinancieros, 3);

        System.out.println("🔍 Clusters detectados: " + clusterResult.clusterCount);
        System.out.println("📊 Distribución por cluster:");
        clusterResult.distribution.forEach((cluster, count) -> 
            System.out.println("   Cluster " + cluster + ": " + count + " elementos")
        );
        System.out.println("⚖️ Clustering balanceado: " + 
            (clusterResult.isBalanced ? "✅ Sí" : "⚠️ No"));

        // Validaciones
        assertEquals(3, clusterResult.clusterCount, "Debería detectar 3 clusters");
        assertTrue(clusterResult.isBalanced, "Los clusters deberían estar balanceados");
        
        // Cada cluster debería tener ~5 elementos
        clusterResult.distribution.values().forEach(count -> 
            assertTrue(count >= 3 && count <= 7, 
                "Cluster desbalanceado con " + count + " elementos")
        );
    }

    @Test
    @Order(8)
    @DisplayName("ML08 - Validación de consistencia con análisis estadístico")
    void testValidacionConsistenciaEstadistica() {
        List<Map<String, Double>> resultadosMultiples = new ArrayList<>();

        // Realizar misma operación múltiples veces
        Map<String, Object> datosBase = new HashMap<>();
        datosBase.put("ingreso", 4000000.0);
        datosBase.put("arriendoHipo", 1200000.0);
        datosBase.put("services", 300000.0);
        datosBase.put("alimentacion", 700000.0);
        datosBase.put("transporte", 400000.0);
        datosBase.put("otros", 200000.0);

        for (int i = 0; i < 20; i++) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", testToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(datosBase, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/info_financiera",
                request,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
            
            Map<String, Double> resultado = new HashMap<>();
            resultado.put("totalGastos", ((Number) gastos.get("totalGastos")).doubleValue());
            resultado.put("disponible", ((Number) gastos.get("disponible")).doubleValue());
            
            resultadosMultiples.add(resultado);
        }

        // 🤖 ANÁLISIS ML: Verificar consistencia (varianza cercana a 0)
        List<Double> totalesGastos = resultadosMultiples.stream()
            .map(r -> r.get("totalGastos"))
            .collect(Collectors.toList());

        List<Double> disponibles = resultadosMultiples.stream()
            .map(r -> r.get("disponible"))
            .collect(Collectors.toList());

        DescriptiveStatistics statsGastos = new DescriptiveStatistics();
        DescriptiveStatistics statsDisponibles = new DescriptiveStatistics();
        
        totalesGastos.forEach(statsGastos::addValue);
        disponibles.forEach(statsDisponibles::addValue);

        double varianzaGastos = statsGastos.getVariance();
        double varianzaDisponibles = statsDisponibles.getVariance();

        System.out.println("📊 Varianza en gastos totales: " + varianzaGastos);
        System.out.println("📊 Varianza en disponibles: " + varianzaDisponibles);
        System.out.println("📊 Desv. Std gastos: " + statsGastos.getStandardDeviation());
        System.out.println("📊 Desv. Std disponibles: " + statsDisponibles.getStandardDeviation());

        // Para mismos inputs, debe haber consistencia perfecta (varianza ~0)
        assertEquals(0.0, varianzaGastos, 0.01, 
            "Resultados inconsistentes para mismos inputs: varianza=" + varianzaGastos);
        assertEquals(0.0, varianzaDisponibles, 0.01,
            "Resultados inconsistentes en disponibles: varianza=" + varianzaDisponibles);

        // Todos los valores deben ser idénticos
        double firstValue = totalesGastos.get(0);
        assertTrue(totalesGastos.stream().allMatch(v -> Math.abs(v - firstValue) < 0.01),
            "Los cálculos no son determinísticos");

        System.out.println("✅ API completamente consistente y determinística");
    }

    // =====================================================
    // CLASES AUXILIARES
    // =====================================================

    private static class ClusterAnalysisResult {
        final int clusterCount;
        final Map<Integer, Integer> distribution;
        final boolean isBalanced;

        ClusterAnalysisResult(int clusterCount, Map<Integer, Integer> distribution, boolean isBalanced) {
            this.clusterCount = clusterCount;
            this.distribution = distribution;
            this.isBalanced = isBalanced;
        }
    }

    private static class OutlierAnalysisResult {
        final List<Integer> outlierIndices;
        final double lowerBound;
        final double upperBound;
        final boolean hasOutliers;

        OutlierAnalysisResult(List<Integer> outlierIndices, double lowerBound, 
                              double upperBound, boolean hasOutliers) {
            this.outlierIndices = outlierIndices;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.hasOutliers = hasOutliers;
        }
    }

    private static class TrendAnalysisResult {
        final String trend;
        final double slope;
        final double intercept;

        TrendAnalysisResult(String trend, double slope, double intercept) {
            this.trend = trend;
            this.slope = slope;
            this.intercept = intercept;
        }
    }
}