from flask import Flask, request, jsonify
from pymongo import MongoClient
from bson import ObjectId
import datetime
import sys
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
from functools import wraps
import os
from typing import TypeVar, Generic, Callable, Any, Union

app = Flask(__name__)
app.config['SECRET_KEY'] = os.urandom(24)  # Clave secreta para JWT

# Variables globales para las colecciones
collection = None
users_collection = None
financial_info_collection = None

# Implementación de Monadas
T = TypeVar('T')
U = TypeVar('U')

class Result(Generic[T]):
    """Monada Result para manejo de errores"""
    
    def __init__(self, value: T = None, error: str = None, is_success: bool = True):
        self.value = value
        self.error = error
        self.is_success = is_success
    
    @classmethod
    def success(cls, value: T) -> 'Result[T]':
        return cls(value=value, is_success=True)
    
    @classmethod
    def failure(cls, error: str) -> 'Result[T]':
        return cls(error=error, is_success=False)
    
    def bind(self, func: Callable[[T], 'Result[U]']) -> 'Result[U]':
        """Operador bind de la monada"""
        if not self.is_success:
            return Result.failure(self.error)
        try:
            return func(self.value)
        except Exception as e:
            return Result.failure(str(e))
    
    def map(self, func: Callable[[T], U]) -> 'Result[U]':
        """Map sobre el valor de la monada"""
        if not self.is_success:
            return Result.failure(self.error)
        try:
            return Result.success(func(self.value))
        except Exception as e:
            return Result.failure(str(e))
    
    def unwrap_or(self, default: T) -> T:
        """Obtiene el valor o un valor por defecto"""
        return self.value if self.is_success else default
    
    def unwrap(self) -> T:
        """Obtiene el valor o lanza excepción"""
        if not self.is_success:
            raise Exception(self.error)
        return self.value

class Maybe(Generic[T]):
    """Monada Maybe para valores opcionales"""
    
    def __init__(self, value: T = None):
        self.value = value
        self.has_value = value is not None
    
    @classmethod
    def some(cls, value: T) -> 'Maybe[T]':
        return cls(value)
    
    @classmethod
    def none(cls) -> 'Maybe[T]':
        return cls(None)
    
    def bind(self, func: Callable[[T], 'Maybe[U]']) -> 'Maybe[U]':
        if not self.has_value:
            return Maybe.none()
        return func(self.value)
    
    def map(self, func: Callable[[T], U]) -> 'Maybe[U]':
        if not self.has_value:
            return Maybe.none()
        try:
            return Maybe.some(func(self.value))
        except:
            return Maybe.none()
    
    def unwrap_or(self, default: T) -> T:
        return self.value if self.has_value else default

# Funciones utilitarias usando monadas
def safe_mongodb_connect() -> Result[bool]:
    """Conexión segura a MongoDB usando Result"""
    global collection, users_collection, financial_info_collection
    try:
        client = MongoClient('mongodb://localhost:27017/', serverSelectionTimeoutMS=5000)
        client.server_info()
        db = client['personas_db']
        collection = db['personas']
        users_collection = db['users']
        financial_info_collection = db['financial_info']
        print("Conexión exitosa a MongoDB")
        return Result.success(True)
    except Exception as e:
        error_msg = f"Error al conectar a MongoDB: {e}"
        print(error_msg, file=sys.stderr)
        return Result.failure(error_msg)

def validate_json_data(request) -> Result[dict]:
    """Validación segura de datos JSON"""
    if not request.is_json:
        return Result.failure("El contenido debe ser JSON")
    
    data = request.get_json()
    if not data:
        return Result.failure("No se recibieron datos JSON válidos")
    
    return Result.success(data)

def validate_user_credentials(data: dict) -> Result[dict]:
    """Validación de credenciales de usuario"""
    if not isinstance(data, dict):
        return Result.failure("Datos inválidos")
    
    if 'email' not in data or 'password' not in data:
        return Result.failure("Email y password son requeridos")
    
    if not isinstance(data['email'], str) or not isinstance(data['password'], str):
        return Result.failure("Email y password deben ser texto")
    
    return Result.success(data)

def find_user_by_email(email: str) -> Maybe[dict]:
    """Búsqueda segura de usuario por email"""
    try:
        user = users_collection.find_one({'email': email})
        return Maybe.some(user) if user else Maybe.none()
    except:
        return Maybe.none()

def validate_financial_data_monad(data: dict) -> Result[dict]:
    """Validación de datos financieros usando monadas"""
    required_fields = ['fuente_principal', 'ingreso_mensual']
    
    # Verificar campos requeridos
    for field in required_fields:
        if field not in data or not data[field]:
            return Result.failure(f"El campo '{field}' es requerido")
    
    # Validar fuente principal
    valid_sources = ['empleo', 'negocio', 'pensión', 'pension', 'otro']
    if data['fuente_principal'].lower() not in valid_sources:
        return Result.failure("La fuente principal debe ser: empleo, negocio, pensión o otro")
    
    # Validar ingreso mensual
    ingreso = data['ingreso_mensual']
    if isinstance(ingreso, str):
        if '-' in ingreso:
            try:
                parts = ingreso.split('-')
                if len(parts) != 2:
                    return Result.failure("El rango de ingreso debe tener formato: 'min-max'")
                float(parts[0].strip())
                float(parts[1].strip())
            except ValueError:
                return Result.failure("El rango de ingreso debe contener números válidos")
        else:
            try:
                float(ingreso)
            except ValueError:
                return Result.failure("El ingreso debe ser un número válido")
    elif not isinstance(ingreso, (int, float)):
        return Result.failure("El ingreso debe ser un número o un rango válido")
    
    # Validar gastos opcionales
    gastos_fields = ['arriendo_hipoteca', 'servicios', 'alimentacion', 'transporte', 'otros_gastos_fijos']
    for field in gastos_fields:
        if field in data and data[field] is not None:
            try:
                float(data[field])
            except (ValueError, TypeError):
                return Result.failure(f"El campo '{field}' debe ser un número válido")
    
    return Result.success(data)

def create_jwt_token(user_email: str) -> Result[str]:
    """Creación segura de token JWT"""
    try:
        token = jwt.encode({
            'email': user_email,
            'exp': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=24)
        }, app.config['SECRET_KEY'], algorithm="HS256")
        return Result.success(token)
    except Exception as e:
        return Result.failure(f"Error al generar token: {str(e)}")

def decode_jwt_token(token: str) -> Result[dict]:
    """Decodificación segura de token JWT"""
    try:
        data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
        return Result.success(data)
    except Exception as e:
        return Result.failure("Token inválido")

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('x-access-token')
        if not token:
            return jsonify({'error': 'Token no proporcionado'}), 401
        
        # Usar monadas para la validación del token
        token_result = (decode_jwt_token(token)
                       .bind(lambda data: Result.success(find_user_by_email(data['email'])))
                       .bind(lambda maybe_user: Result.success(maybe_user.value) if maybe_user.has_value 
                            else Result.failure('Usuario no encontrado')))
        
        if not token_result.is_success:
            return jsonify({'error': 'Token inválido'}), 401
        
        return f(token_result.value, *args, **kwargs)
    
    return decorated

def connect_to_mongodb():
    """Función legacy - mantener compatibilidad"""
    result = safe_mongodb_connect()
    return result.is_success

# Intentar conexión inicial
connect_result = safe_mongodb_connect()
if not connect_result.is_success:
    print("No se pudo conectar a MongoDB. Asegúrate de que MongoDB esté instalado y corriendo.", file=sys.stderr)

@app.route('/info_personal', methods=['POST'])
@token_required
def recibir_datos(current_user):
    global collection
    
    if collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    # Usar monadas para el procesamiento
    result = (validate_json_data(request)
             .map(lambda data: {**data, 
                               'timestamp': datetime.datetime.now(datetime.timezone.utc),
                               'user_id': str(current_user['_id'])})
             .bind(lambda data: Result.success(collection.insert_one(data)))
             .map(lambda db_result: {
                 "mensaje": "Datos recibidos y almacenados correctamente",
                 "id": str(db_result.inserted_id)
             }))
    
    if result.is_success:
        # Logging
        data = request.get_json()
        print("Datos recibidos y almacenados en MongoDB:")
        print(f"Nombre Completo: {data.get('nombreCompleto')}")
        print(f"ID en MongoDB: {result.value['id']}")
        return jsonify(result.value)
    else:
        return jsonify({"error": result.error}), 400

@app.route('/info_financiera', methods=['POST'])
@token_required
def recibir_info_financiera(current_user):
    global financial_info_collection
    
    if financial_info_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500

    # Función de validación específica para datos financieros del cliente Java
    def validate_financial_data_java(data):
        """Valida los datos financieros enviados desde el cliente Java"""
        required_fields = ['fuenteIngreso', 'ingreso']
        errors = []
        
        # Verificar campos requeridos
        for field in required_fields:
            if field not in data:
                errors.append(f"Campo requerido faltante: {field}")
        
        # Validar que ingreso sea un número positivo
        if 'ingreso' in data:
            try:
                ingreso = float(data['ingreso'])
                if ingreso < 0:
                    errors.append("El ingreso no puede ser negativo")
            except (ValueError, TypeError):
                errors.append("El ingreso debe ser un número válido")
        
        # Validar campos de gastos (opcional, pero si están presentes deben ser números)
        gastos_fields = ['arriendoHipo', 'services', 'alimentacion', 'transporte', 'otros']
        for field in gastos_fields:
            if field in data:
                try:
                    valor = float(data[field])
                    if valor < 0:
                        errors.append(f"El campo {field} no puede ser negativo")
                except (ValueError, TypeError):
                    errors.append(f"El campo {field} debe ser un número válido")
        
        # Validar fuente de ingreso
        if 'fuenteIngreso' in data:
            fuentes_validas = [
                'salario/empleo fijo', 'trabajo independiente', 
                'negocio propio', 'pensión', 'inversiones', 'otros'
            ]
            fuente = data['fuenteIngreso'].lower().strip()
            if fuente not in fuentes_validas and fuente != 'selecciona una opción':
                errors.append("Fuente de ingreso no válida")
        
        if errors:
            return Result.failure("; ".join(errors))
        
        return Result.success(data)

    # Procesamiento usando monadas
    def create_financial_data(validated_data):
        # Calcular total de gastos
        total_gastos = sum([
            float(validated_data.get('arriendoHipo', 0)),
            float(validated_data.get('services', 0)),
            float(validated_data.get('alimentacion', 0)),
            float(validated_data.get('transporte', 0)),
            float(validated_data.get('otros', 0))
        ])
        
        ingreso_mensual = float(validated_data['ingreso'])
        disponible = ingreso_mensual - total_gastos
        
        return Result.success({
            'user_id': str(current_user['_id']),
            'fuente_principal': validated_data['fuenteIngreso'].lower().strip(),
            'ingreso_mensual': ingreso_mensual,
            'gastos': {
                'arriendo_hipoteca': float(validated_data.get('arriendoHipo', 0)),
                'servicios': float(validated_data.get('services', 0)),
                'alimentacion': float(validated_data.get('alimentacion', 0)),
                'transporte': float(validated_data.get('transporte', 0)),
                'otros_gastos_fijos': float(validated_data.get('otros', 0))
            },
            'totales': {
                'total_gastos': total_gastos,
                'disponible': disponible
            },
            'timestamp': datetime.datetime.now(datetime.timezone.utc),
            'updated_at': datetime.datetime.now(datetime.timezone.utc)
        })

    def save_financial_data(financial_data):
        try:
            existing_info = financial_info_collection.find_one({'user_id': str(current_user['_id'])})
            
            if existing_info:
                financial_data['created_at'] = existing_info.get('created_at', datetime.datetime.now(datetime.timezone.utc))
                financial_info_collection.replace_one({'user_id': str(current_user['_id'])}, financial_data)
                return Result.success({
                    "action": "actualizada", 
                    "id": str(existing_info['_id']),
                    "financial_data": financial_data
                })
            else:
                financial_data['created_at'] = datetime.datetime.now(datetime.timezone.utc)
                result = financial_info_collection.insert_one(financial_data)
                return Result.success({
                    "action": "creada", 
                    "id": str(result.inserted_id),
                    "financial_data": financial_data
                })
        except Exception as e:
            return Result.failure(f"Error al guardar datos: {str(e)}")

    # Cadena de procesamiento usando monadas
    result = (validate_json_data(request)
              .bind(validate_financial_data_java)
              .bind(create_financial_data)
              .bind(save_financial_data))

    if result.is_success:
        financial_data = result.value['financial_data']
        
        return jsonify({
            "mensaje": f"Información financiera {result.value['action']} correctamente",
            "id": result.value['id'],
            "resumen": {
                "fuente_principal": financial_data['fuente_principal'],
                "ingreso_mensual": financial_data['ingreso_mensual'],
                "total_gastos": financial_data['totales']['total_gastos'],
                "disponible": financial_data['totales']['disponible'],
                "gastos_detallados": financial_data['gastos']
            }
        })
    else:
        return jsonify({"error": result.error}), 400

@app.route('/obtener_info_financiera', methods=['GET'])
@token_required
def obtener_info_financiera(current_user):
    global financial_info_collection
    
    if financial_info_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    try:
        financial_info = financial_info_collection.find_one(
            {'user_id': str(current_user['_id'])},
            {'_id': 0}
        )
        
        maybe_info = Maybe.some(financial_info) if financial_info else Maybe.none()
        
        result = maybe_info.map(lambda info: {
            **info,
            'total_gastos': sum([
                float(info['gastos']['arriendo_hipoteca']),
                float(info['gastos']['servicios']),
                float(info['gastos']['alimentacion']),
                float(info['gastos']['transporte']),
                float(info['gastos']['otros_gastos_fijos'])
            ])
        })
        
        if result.has_value:
            return jsonify({
                "mensaje": "Información financiera obtenida exitosamente",
                "info_financiera": result.value
            })
        else:
            return jsonify({
                "mensaje": "No se encontró información financiera para este usuario",
                "info_financiera": None
            }), 404
    
    except Exception as e:
        return jsonify({"error": f"Error al obtener la información financiera: {str(e)}"}), 500

@app.route('/registro', methods=['POST'])
def registro():
    global users_collection
    
    if users_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    def check_user_exists(data):
        existing_user = find_user_by_email(data['email'])
        if existing_user.has_value:
            return Result.failure("El email ya está registrado")
        return Result.success(data)
    
    def create_user(data):
        try:
            nuevo_usuario = {
                'email': data['email'],
                'password': generate_password_hash(data['password']),
                'created_at': datetime.datetime.now(datetime.timezone.utc)
            }
            result = users_collection.insert_one(nuevo_usuario)
            return Result.success({
                "mensaje": "Usuario registrado exitosamente",
                "id": str(result.inserted_id)
            })
        except Exception as e:
            return Result.failure(f"Error al crear usuario: {str(e)}")
    
    result = (validate_json_data(request)
             .bind(validate_user_credentials)
             .bind(check_user_exists)
             .bind(create_user))
    
    if result.is_success:
        return jsonify(result.value), 201
    else:
        return jsonify({"error": result.error}), 400

@app.route('/login', methods=['POST'])
def login():
    global users_collection
    
    if users_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    def authenticate_user(data):
        maybe_user = find_user_by_email(data['email'])
        if not maybe_user.has_value:
            return Result.failure("Email o contraseña incorrectos")
        
        user = maybe_user.value
        if not check_password_hash(user['password'], data['password']):
            return Result.failure("Email o contraseña incorrectos")
        
        return Result.success(user)
    
    def create_login_response(user):
        token_result = create_jwt_token(user['email'])
        if not token_result.is_success:
            return token_result
        
        return Result.success({
            "mensaje": "Login exitoso",
            "token": token_result.value
        })
    
    result = (validate_json_data(request)
             .bind(validate_user_credentials)
             .bind(authenticate_user)
             .bind(create_login_response))
    
    if result.is_success:
        return jsonify(result.value)
    else:
        return jsonify({"error": result.error}), 401

@app.route('/obtener_info_personal', methods=['GET'])
@token_required
def obtener_personas(current_user):
    global collection
    
    if collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    try:
        personas = list(collection.find({'user_id': str(current_user['_id'])}, {'_id': 0}))
        return jsonify(personas)
    except Exception as e:
        return jsonify({"error": f"Error al obtener los datos: {str(e)}"}), 500

@app.route('/todas_personas', methods=['GET'])
def obtener_todas_personas():
    global collection, users_collection
    
    if collection is None or users_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    try:
        todas_personas = []
        for persona in collection.find():
            persona['_id'] = str(persona['_id'])
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
        return jsonify({"error": f"Error al obtener los datos: {str(e)}"}), 500

@app.route('/toda_info_financiera', methods=['GET'])
def obtener_toda_info_financiera():
    global financial_info_collection, users_collection
    
    if financial_info_collection is None or users_collection is None:
        connect_result = safe_mongodb_connect()
        if not connect_result.is_success:
            return jsonify({"error": connect_result.error}), 500
    
    try:
        toda_info_financiera = []
        for info in financial_info_collection.find():
            info['_id'] = str(info['_id'])
            try:
                usuario = users_collection.find_one({'_id': ObjectId(info['user_id'])})
                if usuario:
                    info['email_usuario'] = usuario['email']
            except:
                info['email_usuario'] = 'No disponible'
            
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
        return jsonify({"error": f"Error al obtener la información financiera: {str(e)}"}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5000)