import React, { useState, useEffect } from 'react';
import { X, CheckCircle, Pin, Send, MessageCircle } from 'lucide-react';
import { reelsService } from '../../services/reelsService';

const CommentDrawer = ({ reelId, isOpen, onClose, onCommentAdded }) => {
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && reelId) {
      fetchComments();
    }
  }, [isOpen, reelId]);

  const fetchComments = async () => {
    setLoading(true);
    try {
      const data = await reelsService.getComments(reelId);
      // Gelen veri bir dizi olabilir veya sayfalanmış (content nesnesi) olabilir
      setComments(Array.isArray(data) ? data : (data?.content || []));
    } catch (error) {
      console.error('Yorumlar yüklenirken hata oluştu:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      await reelsService.addComment(reelId, newComment.trim());
      setNewComment('');
      await fetchComments(); // Yorum eklendikten sonra listeyi yenile
      if (onCommentAdded) {
        onCommentAdded();
      }
    } catch (error) {
      console.error('Yorum eklenirken hata oluştu:', error);
    } finally {
      setSubmitting(false);
    }
  };

  // Drawer kapalıysa render etme
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm transition-opacity">
      {/* Drawer dışına tıklayınca kapanması için arka plan katmanı */}
      <div className="absolute inset-0 cursor-pointer" onClick={onClose}></div>
      
      {/* Drawer paneli */}
      <div className="relative w-full max-w-md bg-neutral-900 h-full shadow-2xl flex flex-col border-l border-neutral-800 animate-slide-in-right">
        
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-neutral-800">
          <div className="flex items-center space-x-2 text-white">
            <MessageCircle className="w-5 h-5 text-neutral-400" />
            <h2 className="text-lg font-semibold">Yorumlar</h2>
            <span className="text-sm font-medium text-neutral-400 bg-neutral-800 px-2.5 py-0.5 rounded-full">
              {comments.length}
            </span>
          </div>
          <button 
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Yorum Listesi */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {loading ? (
            <div className="flex justify-center items-center h-32">
              <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-neutral-500"></div>
            </div>
          ) : comments.length === 0 ? (
            <div className="text-center text-neutral-500 py-10 text-sm">
              Henüz yorum yok. İlk yorumu sen yap!
            </div>
          ) : (
            comments.map((comment) => (
              <div 
                key={comment.id} 
                className="bg-neutral-800/40 rounded-xl p-3.5 flex flex-col space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2 flex-wrap gap-y-1">
                    <span className="font-medium text-white text-sm">
                      @{comment.username || 'kullanici'}
                    </span>
                    
                    {/* Doğrulanmış Alıcı Rozeti */}
                    {comment.isVerifiedBuyer && (
                      <div className="flex items-center space-x-1 bg-sky-500/10 text-sky-400 px-1.5 py-0.5 rounded text-[10px] font-medium border border-sky-500/20">
                        <CheckCircle className="w-3 h-3" />
                        <span>Doğrulanmış Alıcı</span>
                      </div>
                    )}
                  </div>
                  
                  {/* Sabitlendi Rozeti */}
                  {comment.isPinned && (
                    <div className="flex items-center space-x-1 text-amber-500 text-xs font-medium ml-2">
                      <Pin className="w-3 h-3 fill-current" />
                      <span>Sabitlendi</span>
                    </div>
                  )}
                </div>
                <p className="text-neutral-300 text-sm leading-relaxed break-words">
                  {comment.content}
                </p>
              </div>
            ))
          )}
        </div>

        {/* Yorum Ekleme Input Alanı */}
        <div className="p-4 border-t border-neutral-800 bg-neutral-900/95">
          <form onSubmit={handleAddComment} className="flex items-end space-x-2">
            <div className="flex-1 bg-neutral-800 rounded-2xl border border-neutral-700 focus-within:border-neutral-500 transition-colors">
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder="Yorum ekle..."
                className="w-full bg-transparent text-white px-4 py-3 outline-none resize-none max-h-32 text-sm placeholder-neutral-500 scrollbar-hide"
                rows="1"
                onInput={(e) => {
                  e.target.style.height = 'auto';
                  e.target.style.height = (e.target.scrollHeight) + 'px';
                }}
              />
            </div>
            <button
              type="submit"
              disabled={!newComment.trim() || submitting}
              className={`p-3 rounded-full flex items-center justify-center transition-all ${
                newComment.trim() && !submitting
                  ? 'bg-white text-black hover:bg-neutral-200'
                  : 'bg-neutral-800 text-neutral-500 cursor-not-allowed'
              }`}
            >
              <Send className="w-5 h-5 ml-0.5" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CommentDrawer;
