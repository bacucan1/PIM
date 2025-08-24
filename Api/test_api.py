import requests
import json

def test_post_persona():
    url = 'http://localhost:5000/persona'
    data = {
        "nombreCompleto": "Juan Pérez",
        "tipoDocumento": "CC",
        "numeroDocumento": "123456",
        "fechaNacimiento": "2000-01-01",
        "edad": 25,
        "nacionalidad": "Colombiana"
    }
    
    try:
        response = requests.post(url, json=data)
        print("\nPrueba POST /persona:")
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
    except Exception as e:
        print(f"Error en POST: {e}")

def test_get_personas():
    url = 'http://localhost:5000/personas'
    
    try:
        response = requests.get(url)
        print("\nPrueba GET /personas:")
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
    except Exception as e:
        print(f"Error en GET: {e}")

if __name__ == "__main__":
    print("Iniciando pruebas de la API...")
    test_post_persona()
    test_get_personas()
