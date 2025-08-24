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
    global collection, users_collection
    try:
        # Intentar conexión a MongoDB
        client = MongoClient('mongodb://localhost:27017/', serverSelectionTimeoutMS=5000)
        # Forzar una conexión para verificar que MongoDB está corriendo
        client.server_info()
        db = client['personas_db']
        collection = db['personas']
        users_collection = db['users']
        print("Conexión exitosa a MongoDB")
        return True
    except Exception as e:
        print(f"Error al conectar a MongoDB: {e}", file=sys.stderr)
        return False

# Intentar conexión inicial
if not connect_to_mongodb():
    print("No se pudo conectar a MongoDB. Asegúrate de que MongoDB esté instalado y corriendo.", file=sys.stderr)
    # No cerramos la aplicación, permitimos que inicie pero manejamos los errores en los endpoints

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
        data['timestamp'] = datetime.datetime.now()
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
            'created_at': datetime.datetime.now()
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
            'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=24)
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


if __name__ == '__main__':
    app.run(debug=True, port=5000)
