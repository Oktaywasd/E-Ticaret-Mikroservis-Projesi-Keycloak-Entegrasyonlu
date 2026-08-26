import { reelsApi } from '../lib/axios';

export const reelsService = {
  getFeed: async (page = 0, size = 10) => {
    const res = await reelsApi.get(`/reels/feed?page=${page}&size=${size}`);
    return res.data;
  },

  getReelById: async (reelId) => {
    const res = await reelsApi.get(`/reels/${reelId}`);
    return res.data;
  },

  uploadReel: async (formData) => {
    // Interceptor otomatik olarak Authorization: Bearer <token> ekleyecektir
    // Axios formData objesini gördüğünde multipart/form-data ve boundary'yi kendisi ayarlar
    const res = await reelsApi.post('/reels/upload', formData);
    return res.data;
  },

  incrementView: async (reelId) => {
    try {
      await reelsApi.post(`/reels/${reelId}/view`);
    } catch (e) {
      console.warn('View artırılamadı:', e);
    }
  },

  likeReel: async (reelId) => {
    const res = await reelsApi.post(`/reels/${reelId}/like`);
    return res.data;
  },

  getComments: async (reelId) => {
    const res = await reelsApi.get(`/reels/${reelId}/comments`);
    return res.data;
  },

  addComment: async (reelId, content) => {
    const res = await reelsApi.post(`/reels/${reelId}/comments`, { content });
    return res.data;
  },

  pinComment: async (commentId) => {
    await reelsApi.post(`/reels/comments/${commentId}/pin`);
  },

  deleteReel: async (reelId) => {
    const res = await reelsApi.delete(`/reels/${reelId}`);
    return res.data;
  }
};
