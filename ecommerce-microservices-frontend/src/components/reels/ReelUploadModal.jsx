import React, { useState, useEffect } from 'react';
import { X, Upload, Film, Image, Check, AlertCircle } from 'lucide-react';
import { reelsService } from '../../services/reelsService';
import axios from 'axios';

export default function ReelUploadModal({ isOpen, onClose, onUploadSuccess }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [productId, setProductId] = useState('');
  const [videoFile, setVideoFile] = useState(null);
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchingProducts, setFetchingProducts] = useState(false);
  const [error, setError] = useState('');

  // Katalog servisinden satıcının / sistemin ürünlerini çek
  useEffect(() => {
    if (isOpen) {
      const fetchProducts = async () => {
        try {
          setFetchingProducts(true);
          // Product catalog endpoint'ini projedeki yapıya göre çağır
          const res = await axios.get('http://localhost:8081/api/v1/products?size=50');
          const list = res.data?.content || (Array.isArray(res.data) ? res.data : []);
          setProducts(list);
          if (list.length > 0) setProductId(list[0].id);
        } catch (err) {
          console.warn('Ürün listesi çekilemedi:', err);
        } finally {
          setFetchingProducts(false);
        }
      };
      fetchProducts();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!videoFile) {
      setError('Lütfen bir video dosyası seçin.');
      return;
    }
    if (!productId) {
      setError('Lütfen bir ürün seçin.');
      return;
    }

    try {
      setLoading(true);
      setError('');

      const formData = new FormData();
      formData.append('title', title.trim());
      formData.append('description', description.trim());
      formData.append('productId', String(productId));
      formData.append('durationInSeconds', '30');
      formData.append('videoFile', videoFile);
      if (thumbnailFile) {
        formData.append('thumbnailFile', thumbnailFile);
      }

      await reelsService.uploadReel(formData);
      
      setTitle('');
      setDescription('');
      setVideoFile(null);
      setThumbnailFile(null);
      if (onUploadSuccess) onUploadSuccess();
      onClose();
    } catch (err) {
      console.error('Yükleme hatası:', err);
      setError(err.response?.data || err.message || 'Video yüklenemedi.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-2xl p-6 text-white shadow-2xl overflow-y-auto max-h-[90vh]">
        {/* Kapat Butonu */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-neutral-400 hover:text-white rounded-full hover:bg-neutral-800 transition"
        >
          <X size={20} />
        </button>

        <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
          <Film className="text-rose-500" size={22} /> Yeni Reel Yükle
        </h2>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-sm flex items-center gap-2">
            <AlertCircle size={16} /> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">Başlık *</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Örn: Yaz Sezonunun En Çok Satan Tişörtü!"
              className="w-full px-3 py-2 rounded-xl bg-neutral-800 border border-neutral-700 text-sm text-white focus:outline-none focus:border-rose-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">Açıklama *</label>
            <textarea
              required
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ürün detayları, kumaş kalitesi ve kombin tüyoları..."
              className="w-full px-3 py-2 rounded-xl bg-neutral-800 border border-neutral-700 text-sm text-white focus:outline-none focus:border-rose-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">İlişkili Ürün *</label>
            <select
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-neutral-800 border border-neutral-700 text-sm text-white focus:outline-none focus:border-rose-500"
            >
              {fetchingProducts ? (
                <option>Ürünler yükleniyor...</option>
              ) : products.length > 0 ? (
                products.map((p) => (
                  <option key={p.id || p._id} value={p.id || p._id}>
                    {p.name} {p.price ? `- ${typeof p.price === 'object' ? p.price?.amount || '' : p.price || ''} TL` : ''}
                  </option>
                ))
              ) : (
                <option value="">Katalogda ürün bulunamadı</option>
              )}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">Video Dosyası (.mp4, .mov) *</label>
            <input
              type="file"
              required
              accept="video/mp4,video/quicktime"
              onChange={(e) => setVideoFile(e.target.files[0])}
              className="w-full text-xs text-neutral-400 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-neutral-800 file:text-rose-400 hover:file:bg-neutral-700 cursor-pointer"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">Kapak Fotoğrafı (Opsiyonel)</label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setThumbnailFile(e.target.files[0])}
              className="w-full text-xs text-neutral-400 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-neutral-800 file:text-neutral-300 hover:file:bg-neutral-700 cursor-pointer"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-4 py-3 rounded-xl bg-rose-600 hover:bg-rose-500 font-semibold text-sm transition flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {loading ? (
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <Upload size={18} /> Videoyu Yükle
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
