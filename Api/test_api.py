import pytest
import json
import jwt
import datetime
from unittest.mock import Mock, patch
from werkzeug.security import generate_password_hash
from bson import ObjectId

from app import (
    app, connect_to_mongodb, safe_mongodb_connect, 
    Result, Maybe, validate_json_data, validate_user_credentials,
    validate_financial_data_monad,
    create_jwt_token, decode_jwt_token
)

@pytest.fixture
def client():
    """Fixture para crear un cliente de prueba de Flask"""
    app.config['TESTING'] = True
    app.config['SECRET_KEY'] = 'test-secret-key'
    
    with app.test_client() as client:
        with app.app_context():
            yield client

@pytest.fixture
def mock_mongodb():
    """Fixture para mockear las colecciones de MongoDB"""
    with patch('app.collection') as mock_collection, \
         patch('app.users_collection') as mock_users_collection, \
         patch('app.financial_info_collection') as mock_financial_collection:
        
        # Configurar mocks básicos
        mock_collection.insert_one = Mock()
        mock_collection.find = Mock()
        mock_collection.find_one = Mock()
        
        mock_users_collection.insert_one = Mock()
        mock_users_collection.find = Mock()
        mock_users_collection.find_one = Mock()
        
        mock_financial_collection.insert_one = Mock()
        mock_financial_collection.find = Mock()
        mock_financial_collection.find_one = Mock()
        mock_financial_collection.replace_one = Mock()
        
        yield {
            'collection': mock_collection,
            'users_collection': mock_users_collection,
            'financial_info_collection': mock_financial_collection
        }

@pytest.fixture
def valid_user_data():
    """Datos de usuario válidos para las pruebas"""
    return {
        'email': 'test@example.com',
        'password': 'password123'
    }

@pytest.fixture
def valid_personal_data():
    """Datos personales válidos para las pruebas"""
    return {
        'nombreCompleto': 'Juan Pérez',
        'tipoDocumento': 'CC',
        'numeroDocumento': '12345678',
        'fechaNacimiento': '1990-01-01',
        'edad': 33,
        'nacionalidad': 'Colombiana'
    }

@pytest.fixture
def valid_financial_data():
    """Datos financieros válidos para las pruebas"""
    return {
        'fuente_principal': 'empleo',
        'ingreso_mensual': 3000000,
        'arriendo_hipoteca': 800000,
        'servicios': 300000,
        'alimentacion': 500000,
        'transporte': 200000,
        'otros_gastos_fijos': 100000
    }

@pytest.fixture
def auth_token(client, mock_mongodb, valid_user_data):
    """Fixture para generar un token de autenticación válido"""
    # Simular usuario existente en la base de datos
    mock_user = {
        '_id': ObjectId(),
        'email': valid_user_data['email'],
        'password': generate_password_hash(valid_user_data['password'])
    }
    
    mock_mongodb['users_collection'].find_one.return_value = mock_user
    
    # Generar token usando la función moderna
    token = jwt.encode({
        'email': valid_user_data['email'],
        'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=1)
    }, app.config['SECRET_KEY'], algorithm="HS256")
    
    return token, mock_user

class TestMonads:
    """Pruebas específicas para las monadas"""
    
    def test_result_success(self):
        """Prueba Result con operación exitosa"""
        result = Result.success("test_value")
        assert result.is_success
        assert result.value == "test_value"
        assert result.error is None
    
    def test_result_failure(self):
        """Prueba Result con fallo"""
        result = Result.failure("test_error")
        assert not result.is_success
        assert result.error == "test_error"
        assert result.value is None
    
    def test_result_bind_success(self):
        """Prueba bind en Result exitoso"""
        result = Result.success(5)
        new_result = result.bind(lambda x: Result.success(x * 2))
        assert new_result.is_success
        assert new_result.value == 10
    
    def test_result_bind_failure(self):
        """Prueba bind propaga el error"""
        result = Result.failure("error")
        new_result = result.bind(lambda x: Result.success(x * 2))
        assert not new_result.is_success
        assert new_result.error == "error"
    
    def test_result_map_success(self):
        """Prueba map en Result exitoso"""
        result = Result.success(5)
        new_result = result.map(lambda x: x * 2)
        assert new_result.is_success
        assert new_result.value == 10
    
    def test_result_map_exception(self):
        """Prueba map captura excepciones"""
        result = Result.success(5)
        new_result = result.map(lambda x: x / 0)
        assert not new_result.is_success
        assert "division by zero" in new_result.error
    
    def test_maybe_some(self):
        """Prueba Maybe con valor"""
        maybe = Maybe.some("test_value")
        assert maybe.has_value
        assert maybe.value == "test_value"
    
    def test_maybe_none(self):
        """Prueba Maybe sin valor"""
        maybe = Maybe.none()
        assert not maybe.has_value
        assert maybe.value is None
    
    def test_maybe_bind_with_value(self):
        """Prueba bind en Maybe con valor"""
        maybe = Maybe.some(5)
        new_maybe = maybe.bind(lambda x: Maybe.some(x * 2))
        assert new_maybe.has_value
        assert new_maybe.value == 10
    
    def test_maybe_bind_none(self):
        """Prueba bind en Maybe None"""
        maybe = Maybe.none()
        new_maybe = maybe.bind(lambda x: Maybe.some(x * 2))
        assert not new_maybe.has_value

class TestValidationFunctions:
    """Pruebas para funciones de validación usando monadas"""
    
    def test_validate_json_data_success(self):
        """Prueba validación JSON exitosa"""
        mock_request = Mock()
        mock_request.is_json = True
        mock_request.get_json.return_value = {"key": "value"}
        
        result = validate_json_data(mock_request)
        assert result.is_success
        assert result.value == {"key": "value"}
    
    def test_validate_json_data_not_json(self):
        """Prueba validación JSON con contenido no JSON"""
        mock_request = Mock()
        mock_request.is_json = False
        
        result = validate_json_data(mock_request)
        assert not result.is_success
        assert result.error == "El contenido debe ser JSON"
    
    def test_validate_json_data_empty(self):
        """Prueba validación JSON con datos vacíos"""
        mock_request = Mock()
        mock_request.is_json = True
        mock_request.get_json.return_value = None
        
        result = validate_json_data(mock_request)
        assert not result.is_success
        assert result.error == "No se recibieron datos JSON válidos"
    
    def test_validate_user_credentials_success(self, valid_user_data):
        """Prueba validación de credenciales exitosa"""
        result = validate_user_credentials(valid_user_data)
        assert result.is_success
        assert result.value == valid_user_data
    
    def test_validate_user_credentials_missing_email(self):
        """Prueba validación con email faltante"""
        invalid_data = {'password': 'password123'}
        result = validate_user_credentials(invalid_data)
        assert not result.is_success
        assert result.error == "Email y password son requeridos"
    
    def test_validate_user_credentials_invalid_type(self):
        """Prueba validación con tipos inválidos"""
        invalid_data = {'email': 123, 'password': 'password123'}
        result = validate_user_credentials(invalid_data)
        assert not result.is_success
        assert result.error == "Email y password deben ser texto"
    
    def test_validate_financial_data_success(self, valid_financial_data):
        """Prueba validación de datos financieros exitosa"""
        result = validate_financial_data_monad(valid_financial_data)
        assert result.is_success
        assert result.value == valid_financial_data
    
    def test_validate_financial_data_missing_required(self):
        """Prueba validación financiera con campos requeridos faltantes"""
        invalid_data = {'fuente_principal': 'empleo'}  # Falta ingreso_mensual
        result = validate_financial_data_monad(invalid_data)
        assert not result.is_success
        assert "ingreso_mensual" in result.error
    
    def test_validate_financial_data_invalid_source(self):
        """Prueba validación con fuente principal inválida"""
        invalid_data = {
            'fuente_principal': 'invalido',
            'ingreso_mensual': 3000000
        }
        result = validate_financial_data_monad(invalid_data)
        assert not result.is_success
        assert "La fuente principal debe ser" in result.error
    
    def test_validate_financial_data_invalid_income_range(self):
        """Prueba validación con rango de ingreso inválido"""
        invalid_data = {
            'fuente_principal': 'empleo',
            'ingreso_mensual': '1000-2000-3000'  # Formato inválido
        }
        result = validate_financial_data_monad(invalid_data)
        assert not result.is_success
        assert "formato" in result.error.lower()

class TestJWTFunctions:
    """Pruebas para funciones JWT usando monadas"""
    
    def test_create_jwt_token_success(self):
        """Prueba creación exitosa de token"""
        result = create_jwt_token("test@example.com")
        assert result.is_success
        assert isinstance(result.value, str)
    
    def test_decode_jwt_token_success(self):
        """Prueba decodificación exitosa de token"""
        # Crear token válido
        token_result = create_jwt_token("test@example.com")
        assert token_result.is_success
        
        # Decodificar token
        decode_result = decode_jwt_token(token_result.value)
        assert decode_result.is_success
        assert decode_result.value['email'] == "test@example.com"
    
    def test_decode_jwt_token_invalid(self):
        """Prueba decodificación de token inválido"""
        result = decode_jwt_token("invalid_token")
        assert not result.is_success
        assert result.error == "Token inválido"

class TestDatabaseConnection:
    """Pruebas para la conexión a la base de datos"""
    

    @patch('app.MongoClient')
    def test_safe_mongodb_connect_failure(self, mock_mongo_client):
        """Prueba fallo en la conexión usando monadas"""
        mock_mongo_client.side_effect = Exception("Connection failed")
        
        result = safe_mongodb_connect()
        assert not result.is_success
        assert "Connection failed" in result.error
    
    @patch('app.safe_mongodb_connect')
    def test_connect_to_mongodb_legacy_success(self, mock_safe_connect):
        """Prueba función legacy de conexión"""
        mock_safe_connect.return_value = Result.success(True)
        
        result = connect_to_mongodb()
        assert result is True
    
    @patch('app.safe_mongodb_connect')
    def test_connect_to_mongodb_legacy_failure(self, mock_safe_connect):
        """Prueba función legacy de conexión con fallo"""
        mock_safe_connect.return_value = Result.failure("Error")
        
        result = connect_to_mongodb()
        assert result is False

class TestRegistroEndpoint:
    """Pruebas mejoradas para el endpoint de registro"""
    
    def test_registro_exitoso(self, client, mock_mongodb, valid_user_data):
        """Prueba registro exitoso usando monadas"""
        # Simular que el usuario no existe
        mock_mongodb['users_collection'].find_one.return_value = None
        
        # Simular inserción exitosa
        mock_result = Mock()
        mock_result.inserted_id = ObjectId()
        mock_mongodb['users_collection'].insert_one.return_value = mock_result
        
        response = client.post('/registro',
                              data=json.dumps(valid_user_data),
                              content_type='application/json')
        
        assert response.status_code == 201
        data = json.loads(response.data)
        assert data['mensaje'] == 'Usuario registrado exitosamente'
        assert 'id' in data
    
    def test_registro_usuario_existente(self, client, mock_mongodb, valid_user_data):
        """Prueba registro con usuario existente"""
        # Simular que el usuario ya existe
        mock_mongodb['users_collection'].find_one.return_value = {'email': valid_user_data['email']}
        
        response = client.post('/registro',
                              data=json.dumps(valid_user_data),
                              content_type='application/json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert data['error'] == 'El email ya está registrado'
    
    def test_registro_datos_invalidos(self, client, mock_mongodb):
        """Prueba registro con datos inválidos"""
        invalid_data = {'email': 'test@example.com'}  # Falta password
        
        response = client.post('/registro',
                              data=json.dumps(invalid_data),
                              content_type='application/json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'Email y password son requeridos' in data['error']
    

class TestLoginEndpoint:
    """Pruebas mejoradas para el endpoint de login"""
    
    def test_login_exitoso(self, client, mock_mongodb, valid_user_data):
        """Prueba login exitoso con monadas"""
        # Simular usuario existente con contraseña correcta
        mock_user = {
            'email': valid_user_data['email'],
            'password': generate_password_hash(valid_user_data['password'])
        }
        mock_mongodb['users_collection'].find_one.return_value = mock_user
        
        response = client.post('/login',
                              data=json.dumps(valid_user_data),
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['mensaje'] == 'Login exitoso'
        assert 'token' in data
    
    def test_login_credenciales_incorrectas(self, client, mock_mongodb):
        """Prueba login con credenciales incorrectas"""
        # Simular usuario no encontrado
        mock_mongodb['users_collection'].find_one.return_value = None
        
        invalid_data = {'email': 'wrong@example.com', 'password': 'wrongpassword'}
        
        response = client.post('/login',
                              data=json.dumps(invalid_data),
                              content_type='application/json')
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Email o contraseña incorrectos'
    
    def test_login_password_incorrecto(self, client, mock_mongodb, valid_user_data):
        """Prueba login con password incorrecto"""
        # Usuario existe pero password es incorrecto
        mock_user = {
            'email': valid_user_data['email'],
            'password': generate_password_hash('different_password')
        }
        mock_mongodb['users_collection'].find_one.return_value = mock_user
        
        response = client.post('/login',
                              data=json.dumps(valid_user_data),
                              content_type='application/json')
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Email o contraseña incorrectos'

class TestInfoFinancieraEndpoint:
    """Pruebas para el nuevo endpoint de información financiera"""
    
    def test_info_financiera_exitoso(self, client, mock_mongodb, auth_token, valid_financial_data):
        """Prueba guardado exitoso de información financiera"""
        token, mock_user = auth_token
        
        # Simular que no existe información financiera previa
        mock_mongodb['financial_info_collection'].find_one.return_value = None
        
        # Simular inserción exitosa
        mock_result = Mock()
        mock_result.inserted_id = ObjectId()
        mock_mongodb['financial_info_collection'].insert_one.return_value = mock_result
        
        response = client.post('/info_financiera',
                              data=json.dumps(valid_financial_data),
                              content_type='application/json',
                              headers={'x-access-token': token})
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'creada' in data['mensaje']
        assert 'id' in data
        assert 'resumen' in data
    
    def test_info_financiera_actualizacion(self, client, mock_mongodb, auth_token, valid_financial_data):
        """Prueba actualización de información financiera existente"""
        token, mock_user = auth_token
        
        # Simular información financiera existente
        existing_info = {
            '_id': ObjectId(),
            'user_id': str(mock_user['_id']),
            'created_at': datetime.datetime.now(datetime.timezone.utc)
        }
        mock_mongodb['financial_info_collection'].find_one.return_value = existing_info
        
        # Simular actualización exitosa
        mock_result = Mock()
        mock_result.modified_count = 1
        mock_mongodb['financial_info_collection'].replace_one.return_value = mock_result
        
        response = client.post('/info_financiera',
                              data=json.dumps(valid_financial_data),
                              content_type='application/json',
                              headers={'x-access-token': token})
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'actualizada' in data['mensaje']
    
    def test_info_financiera_datos_invalidos(self, client, auth_token):
        """Prueba con datos financieros inválidos"""
        token, _ = auth_token
        
        invalid_data = {
            'fuente_principal': 'fuente_invalida',
            'ingreso_mensual': 'no_es_numero'
        }
        
        response = client.post('/info_financiera',
                              data=json.dumps(invalid_data),
                              content_type='application/json',
                              headers={'x-access-token': token})
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
    
    def test_obtener_info_financiera_exitoso(self, client, mock_mongodb, auth_token):
        """Prueba obtención exitosa de información financiera"""
        token, mock_user = auth_token
        
        # Simular información financiera existente
        mock_financial_info = {
            'user_id': str(mock_user['_id']),
            'fuente_principal': 'empleo',
            'ingreso_mensual': 3000000,
            'gastos': {
                'arriendo_hipoteca': 800000,
                'servicios': 300000,
                'alimentacion': 500000,
                'transporte': 200000,
                'otros_gastos_fijos': 100000
            }
        }
        mock_mongodb['financial_info_collection'].find_one.return_value = mock_financial_info
        
        response = client.get('/obtener_info_financiera',
                             headers={'x-access-token': token})
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['mensaje'] == 'Información financiera obtenida exitosamente'
        assert 'info_financiera' in data
        assert 'total_gastos' in data['info_financiera']
    
    def test_obtener_info_financiera_no_encontrada(self, client, mock_mongodb, auth_token):
        """Prueba cuando no se encuentra información financiera"""
        token, mock_user = auth_token
        
        # Simular que no hay información financiera
        mock_mongodb['financial_info_collection'].find_one.return_value = None
        
        response = client.get('/obtener_info_financiera',
                             headers={'x-access-token': token})
        
        assert response.status_code == 404
        data = json.loads(response.data)
        assert data['info_financiera'] is None

class TestInfoPersonalEndpoint:
    """Pruebas mejoradas para el endpoint de información personal"""
    
    def test_recibir_datos_exitoso(self, client, mock_mongodb, auth_token, valid_personal_data):
        """Prueba envío exitoso de datos personales con monadas"""
        token, mock_user = auth_token
        
        # Simular inserción exitosa
        mock_result = Mock()
        mock_result.inserted_id = ObjectId()
        mock_mongodb['collection'].insert_one.return_value = mock_result
        
        response = client.post('/info_personal',
                              data=json.dumps(valid_personal_data),
                              content_type='application/json',
                              headers={'x-access-token': token})
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['mensaje'] == 'Datos recibidos y almacenados correctamente'
        assert 'id' in data
    


class TestTokenDecorator:
    """Pruebas mejoradas para el decorador token_required"""
    
    def test_token_expirado(self, client, mock_mongodb):
        """Prueba con token expirado usando monadas"""
        # Generar token expirado
        expired_token = jwt.encode({
            'email': 'test@example.com',
            'exp': datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=1)
        }, app.config['SECRET_KEY'], algorithm="HS256")
        
        response = client.get('/obtener_info_personal',
                             headers={'x-access-token': expired_token})
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token inválido'
    
    def test_token_usuario_no_existe(self, client, mock_mongodb):
        """Prueba token válido pero usuario no existe en DB"""
        # Token válido pero usuario no en DB
        valid_token = jwt.encode({
            'email': 'nonexistent@example.com',
            'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=1)
        }, app.config['SECRET_KEY'], algorithm="HS256")
        
        # Simular que no se encuentra usuario
        mock_mongodb['users_collection'].find_one.return_value = None
        
        response = client.get('/obtener_info_personal',
                             headers={'x-access-token': valid_token})
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token inválido'

class TestIntegration:
    """Pruebas de integración usando monadas"""
    
    def test_flujo_completo_usuario(self, client, mock_mongodb):
        """Prueba flujo completo: registro -> login -> guardar datos"""
        user_data = {'email': 'integration@example.com', 'password': 'password123'}
        
        # 1. Registro
        mock_mongodb['users_collection'].find_one.return_value = None
        mock_result = Mock()
        mock_result.inserted_id = ObjectId()
        mock_mongodb['users_collection'].insert_one.return_value = mock_result
        
        registro_response = client.post('/registro',
                                       data=json.dumps(user_data),
                                       content_type='application/json')
        assert registro_response.status_code == 201
        
        # 2. Login
        mock_user = {
            '_id': ObjectId(),
            'email': user_data['email'],
            'password': generate_password_hash(user_data['password'])
        }
        mock_mongodb['users_collection'].find_one.return_value = mock_user
        
        login_response = client.post('/login',
                                    data=json.dumps(user_data),
                                    content_type='application/json')
        assert login_response.status_code == 200
        token = json.loads(login_response.data)['token']
        
        # 3. Guardar información personal
        personal_data = {
            'nombreCompleto': 'Usuario Integración',
            'tipoDocumento': 'CC',
            'numeroDocumento': '12345678'
        }
        
        mock_result = Mock()
        mock_result.inserted_id = ObjectId()
        mock_mongodb['collection'].insert_one.return_value = mock_result
        
        info_response = client.post('/info_personal',
                                   data=json.dumps(personal_data),
                                   content_type='application/json',
                                   headers={'x-access-token': token})
        assert info_response.status_code == 200



# Configuración para ejecutar las pruebas
if __name__ == '__main__':
    pytest.main(['-v', __file__])