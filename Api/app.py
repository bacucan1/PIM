from flask import Flask, request, jsonify
from pymongo import MongoClient
from bson import ObjectId
import datetime
import sys
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
from functools import wraps
import os

app = Flask(__name__)
app.config['SECRET_KEY'] = os.urandom(24)  # Clave secreta para JWT

# Variables globales para las colecciones
collection = None
users_collection = None
financial_info_collection = None

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('x-access-token')
        if not token:
            return jsonify({'error': 'Token no proporcionado'}), 401
        
        try:
            data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
            current_user = users_collection.find_one({'email': data['email']})
            if not current_user:
                return jsonify({'error': 'Token inválido'}), 401
        except:
            return jsonify({'error': 'Token inválido'}), 401
        
        return f(current_user, *args, **kwargs)
    
    return decorated

def connect_to_mongodb():
    global collection, users_collection, financial_info_collection
    try:
        # Intentar conexión a MongoDB
        client = MongoClient('mongodb://localhost:27017/', serverSelectionTimeoutMS=5000)
        # Forzar una conexión para verificar que MongoDB está corriendo
        client.server_info()
        db = client['personas_db']
        collection = db['personas']
        users_collection = db['users']
        financial_info_collection = db['financial_info']
        print("Conexión exitosa a MongoDB")
        return True
    except Exception as e:
        print(f"Error al conectar a MongoDB: {e}", file=sys.stderr)
        return False

def validate_financial_data(data):
    """Valida los datos financieros recibidos"""
    required_fields = ['fuente_principal', 'ingreso_mensual']
    
    # Verificar campos requeridos
    for field in required_fields:
        if field not in data or not data[field]:
            return False, f"El campo '{field}' es requerido"
    
    # Validar fuente principal
    valid_sources = ['empleo', 'negocio', 'pensión', 'pension', 'otro']
    if data['fuente_principal'].lower() not in valid_sources:
        return False, "La fuente principal debe ser: empleo, negocio, pensión o otro"
    
    # Validar ingreso mensual (puede ser un número o un rango)
    ingreso = data['ingreso_mensual']
    if isinstance(ingreso, str):
        # Si es string, verificar que sea un rango válido o un número
        if '-' in ingreso:
            try:
                parts = ingreso.split('-')
                if len(parts) != 2:
                    return False, "El rango de ingreso debe tener formato: 'min-max'"
                float(parts[0].strip())
                float(parts[1].strip())
            except ValueError:
                return False, "El rango de ingreso debe contener números válidos"
        else:
            try:
                float(ingreso)
            except ValueError:
                return False, "El ingreso debe ser un número válido"
    elif not isinstance(ingreso, (int, float)):
        return False, "El ingreso debe ser un número o un rango válido"
    
    # Validar gastos (opcionales pero si existen deben ser números)
    gastos_fields = ['arriendo_hipoteca', 'servicios', 'alimentacion', 'transporte', 'otros_gastos_fijos']
    for field in gastos_fields:
        if field in data and data[field] is not None:
            try:
                float(data[field])
            except (ValueError, TypeError):
                return False, f"El campo '{field}' debe ser un número válido"
    
    return True, ""

# Intentar conexión inicial
if not connect_to_mongodb():
    print("No se pudo conectar a MongoDB. Asegúrate de que MongoDB esté instalado y corriendo.", file=sys.stderr)

@app.route('/info_personal', methods=['POST'])
@token_required
def recibir_datos(current_user):
    global collection
    
    if collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                "error": "No se recibieron datos JSON válidos"
            }), 400
        
        # Agregar timestamp y user_id a los datos
        data['timestamp'] = datetime.datetime.now(datetime.timezone.utc)
        data['user_id'] = str(current_user['_id'])
        
        # Guardar en MongoDB
        result = collection.insert_one(data)
        
        # Imprimir los datos recibidos
        print("Datos recibidos y almacenados en MongoDB:")
        print(f"Nombre Completo: {data.get('nombreCompleto')}")
        print(f"Tipo de Documento: {data.get('tipoDocumento')}")
        print(f"Número de Documento: {data.get('numeroDocumento')}")
        print(f"Fecha de Nacimiento: {data.get('fechaNacimiento')}")
        print(f"Edad: {data.get('edad')}")
        print(f"Nacionalidad: {data.get('nacionalidad')}")
        print(f"ID en MongoDB: {result.inserted_id}")
        print(f"Usuario ID: {current_user['_id']}")
        
        return jsonify({
            "mensaje": "Datos recibidos y almacenados correctamente",
            "id": str(result.inserted_id)
        })
    
    except Exception as e:
        return jsonify({
            "error": f"Error al procesar los datos: {str(e)}"
        }), 500

@app.route('/info_financiera', methods=['POST'])
@token_required
def recibir_info_financiera(current_user):
    """Endpoint para guardar información financiera del usuario"""
    global financial_info_collection
    
    if financial_info_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                "error": "No se recibieron datos JSON válidos"
            }), 400
        
        # Validar los datos financieros
        is_valid, error_message = validate_financial_data(data)
        if not is_valid:
            return jsonify({"error": error_message}), 400
        
        # Preparar datos para guardar
        financial_data = {
            'user_id': str(current_user['_id']),
            'fuente_principal': data['fuente_principal'].lower(),
            'ingreso_mensual': data['ingreso_mensual'],
            'gastos': {
                'arriendo_hipoteca': data.get('arriendo_hipoteca', 0),
                'servicios': data.get('servicios', 0),
                'alimentacion': data.get('alimentacion', 0),
                'transporte': data.get('transporte', 0),
                'otros_gastos_fijos': data.get('otros_gastos_fijos', 0)
            },
            'timestamp': datetime.datetime.now(datetime.timezone.utc),
            'updated_at': datetime.datetime.now(datetime.timezone.utc)
        }
        
        # Verificar si el usuario ya tiene información financiera
        existing_info = financial_info_collection.find_one({'user_id': str(current_user['_id'])})
        
        if existing_info:
            # Actualizar información existente
            financial_data['created_at'] = existing_info.get('created_at', datetime.datetime.now(datetime.timezone.utc))
            result = financial_info_collection.replace_one(
                {'user_id': str(current_user['_id'])}, 
                financial_data
            )
            action = "actualizada"
            record_id = str(existing_info['_id'])
        else:
            # Crear nueva información
            financial_data['created_at'] = datetime.datetime.now(datetime.timezone.utc)
            result = financial_info_collection.insert_one(financial_data)
            action = "creada"
            record_id = str(result.inserted_id)
        
        # Calcular total de gastos
        total_gastos = sum([
            float(financial_data['gastos']['arriendo_hipoteca']),
            float(financial_data['gastos']['servicios']),
            float(financial_data['gastos']['alimentacion']),
            float(financial_data['gastos']['transporte']),
            float(financial_data['gastos']['otros_gastos_fijos'])
        ])
        
        # Imprimir información financiera
        print(f"Información financiera {action} para usuario {current_user['email']}:")
        print(f"Fuente Principal: {financial_data['fuente_principal']}")
        print(f"Ingreso Mensual: {financial_data['ingreso_mensual']}")
        print("Gastos:")
        print(f"  - Arriendo/Hipoteca: {financial_data['gastos']['arriendo_hipoteca']}")
        print(f"  - Servicios: {financial_data['gastos']['servicios']}")
        print(f"  - Alimentación: {financial_data['gastos']['alimentacion']}")
        print(f"  - Transporte: {financial_data['gastos']['transporte']}")
        print(f"  - Otros gastos fijos: {financial_data['gastos']['otros_gastos_fijos']}")
        print(f"  - Total gastos: {total_gastos}")
        
        return jsonify({
            "mensaje": f"Información financiera {action} correctamente",
            "id": record_id,
            "resumen": {
                "fuente_principal": financial_data['fuente_principal'],
                "ingreso_mensual": financial_data['ingreso_mensual'],
                "total_gastos": total_gastos,
                "gastos_detalle": financial_data['gastos']
            }
        })
    
    except Exception as e:
        return jsonify({
            "error": f"Error al procesar la información financiera: {str(e)}"
        }), 500

@app.route('/obtener_info_financiera', methods=['GET'])
@token_required
def obtener_info_financiera(current_user):
    """Endpoint para obtener la información financiera del usuario actual"""
    global financial_info_collection
    
    if financial_info_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        # Buscar información financiera del usuario actual
        financial_info = financial_info_collection.find_one(
            {'user_id': str(current_user['_id'])},
            {'_id': 0}  # Excluir el _id del resultado
        )
        
        if not financial_info:
            return jsonify({
                "mensaje": "No se encontró información financiera para este usuario",
                "info_financiera": None
            }), 404
        
        # Calcular total de gastos
        total_gastos = sum([
            float(financial_info['gastos']['arriendo_hipoteca']),
            float(financial_info['gastos']['servicios']),
            float(financial_info['gastos']['alimentacion']),
            float(financial_info['gastos']['transporte']),
            float(financial_info['gastos']['otros_gastos_fijos'])
        ])
        
        # Agregar el total al response
        financial_info['total_gastos'] = total_gastos
        
        return jsonify({
            "mensaje": "Información financiera obtenida exitosamente",
            "info_financiera": financial_info
        })
    
    except Exception as e:
        return jsonify({
            "error": f"Error al obtener la información financiera: {str(e)}"
        }), 500

# Ruta para registro de usuario
@app.route('/registro', methods=['POST'])
def registro():
    global users_collection
    
    if users_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        if not request.is_json:
            return jsonify({"error": "El contenido debe ser JSON"}), 400
        
        data = request.get_json()
        print("Datos recibidos:", data)  # Debug print
        
        if not data or not isinstance(data, dict):
            return jsonify({"error": "Datos inválidos"}), 400
        
        if 'email' not in data or 'password' not in data:
            return jsonify({"error": "Email y password son requeridos"}), 400
        
        if not isinstance(data['email'], str) or not isinstance(data['password'], str):
            return jsonify({"error": "Email y password deben ser texto"}), 400
        
        # Verificar si el usuario ya existe
        if users_collection.find_one({'email': data['email']}):
            return jsonify({"error": "El email ya está registrado"}), 400
        
        # Crear nuevo usuario
        nuevo_usuario = {
            'email': data['email'],
            'password': generate_password_hash(data['password']),
            'created_at': datetime.datetime.now(datetime.timezone.utc)
        }
        
        result = users_collection.insert_one(nuevo_usuario)
        
        return jsonify({
            "mensaje": "Usuario registrado exitosamente",
            "id": str(result.inserted_id)
        }), 201
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# Ruta para login
@app.route('/login', methods=['POST'])
def login():
    global users_collection
    
    if users_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        data = request.get_json()
        if not data or 'email' not in data or 'password' not in data:
            return jsonify({"error": "Email y password son requeridos"}), 400
        
        usuario = users_collection.find_one({'email': data['email']})
        
        if not usuario or not check_password_hash(usuario['password'], data['password']):
            return jsonify({"error": "Email o contraseña incorrectos"}), 401
        
        # Generar token
        token = jwt.encode({
            'email': usuario['email'],
            'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=24)
        }, app.config['SECRET_KEY'], algorithm="HS256")
        
        return jsonify({
            "mensaje": "Login exitoso",
            "token": token
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# Ruta para obtener todas las personas
@app.route('/obtener_info_personal', methods=['GET'])
@token_required
def obtener_personas(current_user):
    global collection
    
    if collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        # Solo obtener las personas vinculadas al usuario actual
        personas = list(collection.find({'user_id': str(current_user['_id'])}, {'_id': 0}))
        return jsonify(personas)
    except Exception as e:
        return jsonify({
            "error": f"Error al obtener los datos: {str(e)}"
        }), 500

# Ruta pública para obtener todas las personas
@app.route('/todas_personas', methods=['GET'])
def obtener_todas_personas():
    global collection, users_collection
    
    if collection is None or users_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        # Obtener todas las personas con sus datos completos
        todas_personas = []
        for persona in collection.find():
            # Convertir el ObjectId a string para la respuesta JSON
            persona['_id'] = str(persona['_id'])
            # Si la persona tiene un user_id, buscamos el email del usuario
            if 'user_id' in persona:
                try:
                    usuario = users_collection.find_one({'_id': ObjectId(persona['user_id'])})
                    if usuario:
                        persona['email_usuario'] = usuario['email']
                except:
                    persona['email_usuario'] = 'No disponible'
            todas_personas.append(persona)
        
        return jsonify({
            "total_registros": len(todas_personas),
            "personas": todas_personas
        })
    except Exception as e:
        return jsonify({
            "error": f"Error al obtener los datos: {str(e)}"
        }), 500

# Ruta pública para obtener toda la información financiera (solo para administración)
@app.route('/toda_info_financiera', methods=['GET'])
def obtener_toda_info_financiera():
    """Endpoint público para obtener toda la información financiera (usar con precaución)"""
    global financial_info_collection, users_collection
    
    if financial_info_collection is None or users_collection is None:
        if not connect_to_mongodb():
            return jsonify({
                "error": "No hay conexión a MongoDB. Por favor, verifica que el servicio esté corriendo."
            }), 500
    
    try:
        # Obtener toda la información financiera con los emails de usuario
        toda_info_financiera = []
        for info in financial_info_collection.find():
            # Convertir el ObjectId a string para la respuesta JSON
            info['_id'] = str(info['_id'])
            # Buscar el email del usuario
            try:
                usuario = users_collection.find_one({'_id': ObjectId(info['user_id'])})
                if usuario:
                    info['email_usuario'] = usuario['email']
            except:
                info['email_usuario'] = 'No disponible'
            
            # Calcular total de gastos
            total_gastos = sum([
                float(info['gastos']['arriendo_hipoteca']),
                float(info['gastos']['servicios']),
                float(info['gastos']['alimentacion']),
                float(info['gastos']['transporte']),
                float(info['gastos']['otros_gastos_fijos'])
            ])
            info['total_gastos'] = total_gastos
            
            toda_info_financiera.append(info)
        
        return jsonify({
            "total_registros": len(toda_info_financiera),
            "informacion_financiera": toda_info_financiera
        })
    except Exception as e:
        return jsonify({
            "error": f"Error al obtener la información financiera: {str(e)}"
        }), 500

if __name__ == '__main__':
    app.run(debug=True, port=5000)