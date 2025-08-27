import pytest
import json
import jwt
import datetime
from unittest.mock import Mock, patch, MagicMock
from werkzeug.security import generate_password_hash
from bson import ObjectId


from app import app, connect_to_mongodb

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
         patch('app.users_collection') as mock_users_collection:
        
        # Configurar mocks básicos
        mock_collection.insert_one = Mock()
        mock_collection.find = Mock()
        mock_collection.find_one = Mock()
        
        mock_users_collection.insert_one = Mock()
        mock_users_collection.find = Mock()
        mock_users_collection.find_one = Mock()
        
        yield {
            'collection': mock_collection,
            'users_collection': mock_users_collection
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
def auth_token(client, mock_mongodb, valid_user_data):
    """Fixture para generar un token de autenticación válido"""
    # Simular usuario existente en la base de datos
    mock_user = {
        '_id': ObjectId(),
        'email': valid_user_data['email'],
        'password': generate_password_hash(valid_user_data['password'])
    }
    
    mock_mongodb['users_collection'].find_one.return_value = mock_user
    
    # Generar token
    token = jwt.encode({
        'email': valid_user_data['email'],
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=1)
    }, app.config['SECRET_KEY'], algorithm="HS256")
    
    return token, mock_user

class TestDatabaseConnection:
    """Pruebas para la conexión a la base de datos"""
    
    
    @patch('app.MongoClient')
    def test_connect_to_mongodb_failure(self, mock_mongo_client):
        """Prueba fallo en la conexión a MongoDB"""
        mock_mongo_client.side_effect = Exception("Connection failed")
        
        result = connect_to_mongodb()
        
        assert result is False

class TestRegistroEndpoint:
    """Pruebas para el endpoint de registro"""
    
    def test_registro_exitoso(self, client, mock_mongodb, valid_user_data):
        """Prueba registro exitoso de usuario"""
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
        """Prueba registro de usuario que ya existe"""
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
    
    def test_registro_sin_json(self, client):
        """Prueba registro sin enviar JSON"""
        response = client.post('/registro', data='not json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert data['error'] == 'El contenido debe ser JSON'

class TestLoginEndpoint:
    """Pruebas para el endpoint de login"""
    
    def test_login_exitoso(self, client, mock_mongodb, valid_user_data):
        """Prueba login exitoso"""
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
    
    def test_login_datos_faltantes(self, client, mock_mongodb):
        """Prueba login sin email o password"""
        incomplete_data = {'email': 'test@example.com'}
        
        response = client.post('/login',
                              data=json.dumps(incomplete_data),
                              content_type='application/json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert data['error'] == 'Email y password son requeridos'

class TestInfoPersonalEndpoint:
    """Pruebas para el endpoint de información personal"""
    
    def test_recibir_datos_exitoso(self, client, mock_mongodb, auth_token, valid_personal_data):
        """Prueba envío exitoso de datos personales"""
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
    
    def test_recibir_datos_sin_token(self, client, valid_personal_data):
        """Prueba envío de datos sin token de autenticación"""
        response = client.post('/info_personal',
                              data=json.dumps(valid_personal_data),
                              content_type='application/json')
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token no proporcionado'
    
    def test_recibir_datos_token_invalido(self, client, valid_personal_data):
        """Prueba envío de datos con token inválido"""
        response = client.post('/info_personal',
                              data=json.dumps(valid_personal_data),
                              content_type='application/json',
                              headers={'x-access-token': 'token_invalido'})
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token inválido'
    
    

class TestObtenerInfoPersonalEndpoint:
    """Pruebas para el endpoint de obtener información personal"""
    
    def test_obtener_personas_exitoso(self, client, mock_mongodb, auth_token):
        """Prueba obtención exitosa de personas"""
        token, mock_user = auth_token
        
        # Simular datos devueltos por la base de datos
        mock_personas = [
            {'nombreCompleto': 'Juan Pérez', 'edad': 30},
            {'nombreCompleto': 'María García', 'edad': 25}
        ]
        mock_mongodb['collection'].find.return_value = mock_personas
        
        response = client.get('/obtener_info_personal',
                             headers={'x-access-token': token})
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert len(data) == 2
        assert data[0]['nombreCompleto'] == 'Juan Pérez'
    
    def test_obtener_personas_sin_token(self, client):
        """Prueba obtención de personas sin token"""
        response = client.get('/obtener_info_personal')
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token no proporcionado'

class TestTodasPersonasEndpoint:
    """Pruebas para el endpoint público de todas las personas"""
    
    def test_obtener_todas_personas_exitoso(self, client, mock_mongodb):
        """Prueba obtención exitosa de todas las personas"""
        # Simular datos con ObjectId
        mock_personas = [
            {
                '_id': ObjectId(),
                'nombreCompleto': 'Juan Pérez',
                'user_id': str(ObjectId())
            }
        ]
        
        # Simular usuario asociado
        mock_user = {'email': 'test@example.com'}
        mock_mongodb['users_collection'].find_one.return_value = mock_user
        mock_mongodb['collection'].find.return_value = mock_personas
        
        response = client.get('/todas_personas')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'total_registros' in data
        assert 'personas' in data
        assert data['total_registros'] >= 0

class TestTokenDecorator:
    """Pruebas para el decorador token_required"""
    
    def test_token_expirado(self, client, mock_mongodb):
        """Prueba con token expirado"""
        # Generar token expirado
        expired_token = jwt.encode({
            'email': 'test@example.com',
            'exp': datetime.datetime.utcnow() - datetime.timedelta(hours=1)
        }, app.config['SECRET_KEY'], algorithm="HS256")
        
        response = client.get('/obtener_info_personal',
                             headers={'x-access-token': expired_token})
        
        assert response.status_code == 401
        data = json.loads(response.data)
        assert data['error'] == 'Token inválido'

class TestErrorHandling:
    """Pruebas para manejo de errores"""
    
    @patch('app.collection', None)
    @patch('app.connect_to_mongodb')
    def test_mongodb_no_disponible(self, mock_connect, client, auth_token):
        """Prueba cuando MongoDB no está disponible"""
        token, _ = auth_token
        mock_connect.return_value = False
        
        response = client.post('/info_personal',
                              data=json.dumps({'test': 'data'}),
                              content_type='application/json',
                              headers={'x-access-token': token})
        
        assert response.status_code == 500
        data = json.loads(response.data)
        assert 'MongoDB' in data['error']

# Configuración para ejecutar las pruebas
if __name__ == '__main__':
    pytest.main(['-v', __file__])