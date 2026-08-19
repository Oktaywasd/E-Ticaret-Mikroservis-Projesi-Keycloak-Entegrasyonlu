import requests

# Keycloak Ayarları
KEYCLOAK_TOKEN_URL = "http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token"
KEYCLOAK_CLIENT_ID = "ecommerce-frontend"   # Keycloak Realm'inde tanımlı Client ID
USERNAME = "admin_seller"                    # Keycloak'taki kullanıcı adın
PASSWORD = "123456"                       # Keycloak'taki kullanıcı şifren

def test_auth():
    payload = {
        "client_id": KEYCLOAK_CLIENT_ID,
        "grant_type": "password",
        "username": USERNAME,
        "password": PASSWORD
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}

    print(f"Keycloak'a istek atılıyor: {KEYCLOAK_TOKEN_URL}...")
    try:
        res = requests.post(KEYCLOAK_TOKEN_URL, data=payload, headers=headers)
        if res.status_code == 200:
            token = res.json().get("access_token")
            print("\n✅ BAŞARILI! Keycloak JWT Token alındı.")
            print(f"Token İlk 50 karakter: {token[:50]}...")
            return token
        else:
            print(f"\n❌ BAŞARISIZ! HTTP Kodu: {res.status_code}")
            print(f"Hata Detayı: {res.text}")
    except Exception as e:
        print(f"\n❌ Bağlantı Hatası: Keycloak konteyneri ayakta mı? Hata: {e}")

if __name__ == "__main__":
    test_auth()