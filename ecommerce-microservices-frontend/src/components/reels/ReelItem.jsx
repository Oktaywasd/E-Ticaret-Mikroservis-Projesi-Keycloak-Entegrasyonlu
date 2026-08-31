import React, { useRef, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, MessageCircle, Eye, ShoppingBag, Volume2, VolumeX, Play } from 'lucide-react';
import { reelsService } from '../../services/reelsService';

export default function ReelItem({ reel, isActive, onOpenComments, commentCount }) {
  const videoRef = useRef(null);
  const [isLiked, setIsLiked] = useState(reel?.isLiked || false);
  const [likes, setLikes] = useState(reel?.likeCount || 0);
  const [views, setViews] = useState(reel?.viewCount || 0);
  const [isMuted, setIsMuted] = useState(true);
  const [isPlaying, setIsPlaying] = useState(true);
  const hasViewedRef = useRef(false);

  const navigate = useNavigate();

  // Sayı formatlayıcı (Örn: 1.4K, 28.5K)
  const formatCount = (num) => {
    if (num === undefined || num === null) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
  };

  // Video Aktif Olduğunda Oynat & İzlenme Artır (Tek seferlik)
  useEffect(() => {
    if (videoRef.current) {
      if (isActive) {
        videoRef.current.currentTime = 0;
        videoRef.current.play().catch(() => {});
        setIsPlaying(true);

        if (!hasViewedRef.current) {
          hasViewedRef.current = true;
          setViews((prev) => prev + 1);
          reelsService.incrementView(reel.id);
        }
      } else {
        videoRef.current.pause();
        setIsPlaying(false);
      }
    }
  }, [isActive, reel?.id]);

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  const handleLike = async (e) => {
    e.stopPropagation();
    
    // Optimistic UI Güncellemesi
    const nextIsLiked = !isLiked;
    setIsLiked(nextIsLiked);
    setLikes((prev) => (nextIsLiked ? prev + 1 : Math.max(0, prev - 1)));

    try {
      await reelsService.likeReel(reel.id);
    } catch (err) {
      console.error('Like hatası:', err);
      // Hata durumunda eski değere geri al (Rollback)
      setIsLiked(!nextIsLiked);
      setLikes((prev) => (!nextIsLiked ? prev + 1 : Math.max(0, prev - 1)));
      if (err.response?.status === 401 || err.response?.status === 403) {
        alert('Beğenmek için giriş yapmalısınız.');
      }
    }
  };

  const handleProductClick = (e) => {
    e.stopPropagation();
    const pId = reel?.productId || reel?.product?.id;
    if (pId) {
      navigate('/products/' + pId);
    }
  };

  return (
    <div className="relative w-full h-full bg-black flex items-center justify-center overflow-hidden select-none" data-testid="reels-video-card">
      {/* Video */}
      <video
        ref={videoRef}
        src={reel?.videoUrl}
        poster={reel?.thumbnailUrl}
        loop
        muted={isMuted}
        playsInline
        onClick={togglePlay}
        className="w-full h-full object-cover cursor-pointer"
      />

      {/* Oynat/Duraklat İkonu */}
      {!isPlaying && (
        <div onClick={togglePlay} className="absolute inset-0 flex items-center justify-center bg-black/20 cursor-pointer z-10">
          <div className="p-4 rounded-full bg-black/50 text-white backdrop-blur-md">
            <Play size={36} className="fill-white translate-x-0.5" />
          </div>
        </div>
      )}

      {/* Ses Aç / Kapa Butonu */}
      <button
        onClick={(e) => { e.stopPropagation(); setIsMuted(!isMuted); }}
        className="absolute top-4 right-4 z-20 p-2.5 rounded-full bg-black/40 backdrop-blur-md text-white hover:bg-black/60 transition"
      >
        {isMuted ? <VolumeX size={18} /> : <Volume2 size={18} />}
      </button>

      {/* Sağ Yan Sayaç ve Aksiyon Butonları */}
      <div className="absolute right-3 bottom-20 z-20 flex flex-col items-center gap-4">
        {/* Beğeni */}
        <button onClick={handleLike} className="flex flex-col items-center group" data-testid="reels-like-button">
          <div className={`p-3 rounded-full backdrop-blur-md transition ${isLiked ? 'bg-rose-600/90 text-white' : 'bg-black/40 text-white group-hover:bg-black/60'}`}>
            <Heart size={24} className={isLiked ? 'fill-white' : ''} />
          </div>
          <span className="text-white text-xs font-semibold mt-1 drop-shadow">{formatCount(likes)}</span>
        </button>

        {/* Yorum */}
        <button onClick={(e) => { e.stopPropagation(); onOpenComments(); }} className="flex flex-col items-center group" data-testid="reels-comment-button">
          <div className="p-3 rounded-full bg-black/40 backdrop-blur-md text-white group-hover:bg-black/60 transition">
            <MessageCircle size={24} />
          </div>
          <span className="text-white text-xs font-semibold mt-1 drop-shadow">{formatCount(commentCount !== undefined ? commentCount : reel?.commentCount || 0)}</span>
        </button>

        {/* İzlenme */}
        <div className="flex flex-col items-center">
          <div className="p-3 rounded-full bg-black/40 backdrop-blur-md text-white">
            <Eye size={22} />
          </div>
          <span className="text-white text-xs font-semibold mt-1 drop-shadow">{formatCount(views)}</span>
        </div>
      </div>

      {/* Sol Alt Ürün & Açıklama */}
      <div className="absolute left-4 bottom-6 right-20 z-20 text-white pointer-events-auto">
        <h4 className="font-bold text-base drop-shadow-md">{reel?.title}</h4>
        <p className="text-xs text-neutral-300 line-clamp-2 mt-1 drop-shadow">{reel?.description}</p>

        <div onClick={handleProductClick} className="mt-3 flex items-center gap-3 p-2.5 rounded-xl bg-black/50 backdrop-blur-md border border-white/10 w-fit max-w-[280px] cursor-pointer hover:bg-black/70 transition">
          <div className="p-2 bg-rose-600 rounded-lg text-white">
            <ShoppingBag size={18} />
          </div>
          <div className="flex flex-col">
            <span className="text-[10px] text-neutral-400 font-medium">Öne Çıkan Ürün</span>
            <span className="text-xs font-semibold text-white truncate">{reel?.product?.name || 'Ürünü İncele'}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
