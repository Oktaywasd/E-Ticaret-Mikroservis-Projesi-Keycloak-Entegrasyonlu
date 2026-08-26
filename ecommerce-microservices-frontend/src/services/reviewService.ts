import { productApi } from '@/lib/axios';

export interface Review {
  id: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  verifiedBuyer: boolean;
  adminReply?: string;
  createdAt: string;
}

export interface AdminReply {
  replyText: string;
  repliedBy: string;
  repliedAt: string;
}

export interface Question {
  id: string;
  userId: string;
  userName: string;
  comment: string;
  adminReply?: AdminReply;
  createdAt: string;
}

export interface CreateReviewData {
  rating: number;
  comment: string;
}

export interface CreateQuestionData {
  question: string;
}

export const reviewService = {
  getReviews: async (productId: string): Promise<Review[]> => {
    const { data } = await productApi.get(`/products/${productId}/reviews`);
    return data;
  },

  getQuestions: async (productId: string): Promise<Question[]> => {
    const { data } = await productApi.get(`/products/${productId}/questions`);
    return data;
  },

  createReview: async (productId: string, reviewData: CreateReviewData): Promise<Review> => {
    const { data } = await productApi.post(`/products/${productId}/reviews`, reviewData);
    return data;
  },

  createQuestion: async (productId: string, questionData: CreateQuestionData): Promise<Question> => {
    const { data } = await productApi.post(`/products/${productId}/questions`, questionData);
    return data;
  },

  replyToReview: async (productId: string, reviewId: string, replyText: string): Promise<Review> => {
    const { data } = await productApi.post(`/products/${productId}/reviews/${reviewId}/reply`, { replyText });
    return data;
  },

  replyToQuestion: async (productId: string, questionId: string, replyText: string): Promise<Question> => {
    const { data } = await productApi.post(`/products/${productId}/questions/${questionId}/reply`, { replyText });
    return data;
  },
};
