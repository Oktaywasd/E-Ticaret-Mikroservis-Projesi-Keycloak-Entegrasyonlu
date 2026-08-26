import React, { useState, useEffect } from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Mousewheel } from 'swiper/modules';
import 'swiper/css';
import { Plus } from 'lucide-react';

import { reelsService } from '../services/reelsService';
import ReelItem from '../components/reels/ReelItem';
import CommentDrawer from '../components/reels/CommentDrawer';
import ReelUploadModal from '../components/reels/ReelUploadModal';
import { useAppAuth } from '../hooks/useAppAuth';

export default function ReelsFeedPage() {
  const [reels, setReels] = useState([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [selectedReelId, setSelectedReelId] = useState(null);
  const [isCommentOpen, setIsCommentOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [commentCounts, setCommentCounts] = useState({});
  const [loading, setLoading] = useState(true);

  const { isAdminOrSeller } = useAppAuth();

  const fetchReels = async (isMounted = true) => {
    try {
      setLoading(true);
      const res = await reelsService.getFeed(0, 10);
      const list = res?.content || (Array.isArray(res) ? res : []);
      
      if (isMounted) {
        setReels(list);
        const counts = {};
        list.forEach(r => { counts[r.id] = r.commentCount || 0; });
        setCommentCounts(counts);
      }
    } catch (err) {
      console.error('Reels feed hatası:', err);
    } finally {
      if (isMounted) setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    fetchReels(isMounted);
    return () => { isMounted = false; };
  }, []);

  const handleCommentAdded = (reelId) => {
    setCommentCounts((prev) => ({
      ...prev,
      [reelId]: (prev[reelId] || 0) + 1
    }));
  };

  if (loading && reels.length === 0) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-80px)] text-white">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-rose-500"></div>
      </div>
    );
  }

  return (
    <div className="relative w-full h-[calc(100vh-80px)] max-w-md mx-auto bg-black rounded-2xl overflow-hidden shadow-2xl my-2">
      
      {(!reels || reels.length === 0) ? (
        <div className="flex flex-col items-center justify-center h-full text-neutral-400">
          <p className="text-lg font-medium">Henüz hiç reel bulunamadı.</p>
        </div>
      ) : (
        <Swiper
          direction="vertical"
          slidesPerView={1}
          spaceBetween={0}
          mousewheel={true}
          modules={[Mousewheel]}
          onSlideChange={(swiper) => setActiveIndex(swiper.activeIndex)}
          className="w-full h-full"
        >
          {reels.map((reel, index) => (
            <SwiperSlide key={reel.id || index}>
              <ReelItem
                reel={reel}
                isActive={index === activeIndex}
                commentCount={commentCounts[reel.id]}
                onOpenComments={() => {
                  setSelectedReelId(reel.id);
                  setIsCommentOpen(true);
                }}
              />
            </SwiperSlide>
          ))}
        </Swiper>
      )}

      {/* Yeni Reel Ekle Butonu */}
      {isAdminOrSeller && (
        <button
          onClick={() => setIsUploadModalOpen(true)}
          className="absolute top-4 right-4 z-30 flex items-center gap-2 bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-full font-medium shadow-lg transition transform hover:scale-105"
        >
          <Plus size={20} />
          <span className="hidden sm:inline">Video Yükle</span>
        </button>
      )}

      <CommentDrawer
        reelId={selectedReelId}
        isOpen={isCommentOpen}
        onClose={() => setIsCommentOpen(false)}
        onCommentAdded={() => handleCommentAdded(selectedReelId)}
      />

      <ReelUploadModal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onUploadSuccess={() => fetchReels(true)}
      />
    </div>
  );
}
