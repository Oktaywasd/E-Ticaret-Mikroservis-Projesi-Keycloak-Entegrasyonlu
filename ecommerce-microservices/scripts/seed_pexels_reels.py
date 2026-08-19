import os
import requests

# 1. Konfigürasyon
PEXELS_API_KEY = "FxAmsZfnkhZ8AnQfPSG2bZCJXGuamGLyMdOVfPE8PSrkpv6P3ohoDxVR"
MEDIA_SERVICE_UPLOAD_URL = "http://localhost:8084/api/v1/reels/upload"
KEYCLOAK_TOKEN_URL = "http://localhost:8080/realms/ecommerce-realm/protocol/openid-connect/token"

# Az önce başarıyla doğruladığımız Keycloak bilgileri
KEYCLOAK_CLIENT_ID = "ecommerce-frontend"
KEYCLOAK_USERNAME = "admin_seller"
KEYCLOAK_PASSWORD = "123456"

def get_keycloak_token():
    """Keycloak üzerinden yetkili Bearer JWT token alır"""
    payload = {
        "client_id": KEYCLOAK_CLIENT_ID,
        "grant_type": "password",
        "username": KEYCLOAK_USERNAME,
        "password": KEYCLOAK_PASSWORD
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    res = requests.post(KEYCLOAK_TOKEN_URL, data=payload, headers=headers)

    if res.status_code == 200:
        return res.json().get("access_token")
    else:
        raise Exception(f"Keycloak Token Hatası ({res.status_code}): {res.text}")

def run_seeder():
    # 1. Adım: Keycloak Token Al
    print("1. Keycloak'tan yetkili JWT Token alınıyor...")
    token = get_keycloak_token()
    auth_headers = {"Authorization": f"Bearer {token}"}
    print("✅ Keycloak Token alındı!\n")

    # 2. Adım: Pexels API'den Dikey Moda Videoları Çek
    print("2. Pexels API'den dikey moda videoları çekiliyor...")
    pexels_headers = {"Authorization": PEXELS_API_KEY}
    params = {
        "query": "fashion model outfit street style",
        "orientation": "portrait",
        "per_page": 5
    }

    pexels_res = requests.get("https://api.pexels.com/videos/search", headers=pexels_headers, params=params)
    if pexels_res.status_code != 200:
        print(f"❌ Pexels API Hatası: {pexels_res.text}")
        return

    videos = pexels_res.json().get("videos", [])
    print(f"✅ {len(videos)} adet dikey video bulundu. API yükleme döngüsü başlıyor...\n")

    # 3. Adım: Videoları İndir ve Discovery Media Service API'ye Yükle
    titles = [
        "Oversize Sokak Stili",
        "Sonbahar Deri Ceket Kombini",
        "Minimalist Yaz Koleksiyonu",
        "Retro Sneaker İnceleme",
        "Vintage Denim Ceket"
    ]

    for idx, video in enumerate(videos):
        video_files = video.get("video_files", [])
        # HD veya 720p dikey MP4 formatını seç
        selected_file = next((f for f in video_files if f.get("width") == 720 or f.get("quality") == "hd"), video_files[0])

        video_url = selected_file.get("link")
        thumbnail_url = video.get("image")
        duration = video.get("duration", 15)
        title = titles[idx % len(titles)]

        print(f"[{idx+1}/{len(videos)}] İndiriliyor: {title} (Video ID: {video.get('id')})...")

        # Geçici dosya olarak diske kaydet
        video_temp = f"temp_video_{idx}.mp4"
        thumb_temp = f"temp_thumb_{idx}.jpg"

        with open(video_temp, "wb") as f:
            f.write(requests.get(video_url).content)
        with open(thumb_temp, "wb") as f:
            f.write(requests.get(thumbnail_url).content)

        payload_data = {
            "title": title,
            "description": f"{title} ile tarzını oluştur! Detaylar için ürüne göz atın. #trend #kombin",
            "productId": f"prod_{idx+1}",
            "durationInSeconds": duration
        }

        # Media Service'e Multipart Upload (Spring Security + Keycloak korumalı)
        print(f"   -> Media Service API'ye yükleniyor (POST /api/v1/reels/upload)...")
        with open(video_temp, "rb") as vf, open(thumb_temp, "rb") as tf:
            files = {
                "video": (video_temp, vf, "video/mp4"),
                "thumbnail": (thumb_temp, tf, "image/jpeg")
            }

            upload_res = requests.post(
                MEDIA_SERVICE_UPLOAD_URL,
                data=payload_data,
                files=files,
                headers=auth_headers
            )

            if upload_res.status_code in [200, 201]:
                res_data = upload_res.json()
                print(f"   ✅ Başarıyla yüklendi! Reel ID: {res_data.get('id')} | MinIO: {res_data.get('videoUrl')}\n")
            else:
                print(f"   ❌ Yükleme Başarısız! Kod: {upload_res.status_code} - Cevap: {upload_res.text}\n")

        # Geçici dosyaları sil
        if os.path.exists(video_temp): os.remove(video_temp)
        if os.path.exists(thumb_temp): os.remove(thumb_temp)

    print("🎉 Tüm işlemler başarıyla tamamlandı! Reels verileri hazır.")

if __name__ == "__main__":
    run_seeder()