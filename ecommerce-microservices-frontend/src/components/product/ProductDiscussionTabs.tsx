import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle, MessageSquare, ThumbsUp, Send } from 'lucide-react';
import { toast } from 'sonner';

import { reviewService } from '@/services/reviewService';
import type { Review, Question } from '@/services/reviewService';
import { StarRating } from '@/components/common/StarRating';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { ReviewModal } from './ReviewModal';

import { useAuth } from 'react-oidc-context';
import { hasRole } from '@/lib/auth';

interface ProductDiscussionTabsProps {
  productId: string;
}

export function ProductDiscussionTabs({ productId }: ProductDiscussionTabsProps) {
  const [activeTab, setActiveTab] = useState<'reviews' | 'questions'>('reviews');
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [newQuestion, setNewQuestion] = useState('');

  const [replyingToReview, setReplyingToReview] = useState<string | null>(null);
  const [reviewReplyText, setReviewReplyText] = useState('');

  const [replyingToQuestion, setReplyingToQuestion] = useState<string | null>(null);
  const [questionReplyText, setQuestionReplyText] = useState('');

  const queryClient = useQueryClient();
  const auth = useAuth();
  const isAdminOrSeller = auth.isAuthenticated && hasRole(auth, ['ADMIN', 'SELLER', 'ROLE_ADMIN', 'ROLE_SELLER'] as any[]);

  if (!productId) return null;

  // Queries
  const { data: reviews = [], isLoading: isLoadingReviews } = useQuery({
    queryKey: ['reviews', productId],
    queryFn: () => reviewService.getReviews(productId),
  });

  const { data: questions = [], isLoading: isLoadingQuestions } = useQuery({
    queryKey: ['questions', productId],
    queryFn: () => reviewService.getQuestions(productId),
  });

  // Mutations
  const questionMutation = useMutation({
    mutationFn: (questionText: string) => reviewService.createQuestion(productId, { question: questionText }),
    onSuccess: () => {
      toast.success('Sorunuz başarıyla gönderildi.');
      setNewQuestion('');
      queryClient.invalidateQueries({ queryKey: ['questions', productId] });
    },
    onError: () => {
      toast.error('Soru gönderilirken bir hata oluştu.');
    }
  });

  const replyToReviewMutation = useMutation({
    mutationFn: ({ reviewId, text }: { reviewId: string; text: string }) => reviewService.replyToReview(productId, reviewId, text),
    onSuccess: () => {
      toast.success('Yorum yanıtlandı.');
      setReplyingToReview(null);
      setReviewReplyText('');
      queryClient.invalidateQueries({ queryKey: ['reviews', productId] });
    },
    onError: () => toast.error('Yanıt gönderilemedi.')
  });

  const replyToQuestionMutation = useMutation({
    mutationFn: ({ questionId, text }: { questionId: string; text: string }) => reviewService.replyToQuestion(productId, questionId, text),
    onSuccess: () => {
      toast.success('Soru yanıtlandı.');
      setReplyingToQuestion(null);
      setQuestionReplyText('');
      queryClient.invalidateQueries({ queryKey: ['questions', productId] });
    },
    onError: () => toast.error('Yanıt gönderilemedi.')
  });

  const handleAskQuestion = () => {
    if (!newQuestion.trim()) {
      toast.error('Lütfen bir soru metni girin.');
      return;
    }
    questionMutation.mutate(newQuestion);
  };

  const handleReviewSuccess = () => {
    queryClient.invalidateQueries({ queryKey: ['reviews', productId] });
    queryClient.invalidateQueries({ queryKey: ['product', productId] }); // Update avg rating in product detail
    // Yorum sonrası vitrinleri ve listeleri anında tetikle
    queryClient.invalidateQueries({ queryKey: ['top-products'] });
    queryClient.invalidateQueries({ queryKey: ['top-50-products'] });
    queryClient.invalidateQueries({ queryKey: ['products'] });
    queryClient.invalidateQueries({ queryKey: ['product'] });
  };

  return (
    <div className="mt-12 rounded-xl border border-border/50 bg-card overflow-hidden">
      {/* Tabs Header */}
      <div className="flex border-b border-border/50">
        <button
          onClick={() => setActiveTab('reviews')}
          className={`flex-1 py-4 text-sm font-medium transition-colors ${
            activeTab === 'reviews'
              ? 'border-b-2 border-violet-500 text-violet-400 bg-violet-500/5'
              : 'text-muted-foreground hover:bg-muted/30 hover:text-foreground'
          }`}
        >
          Değerlendirmeler & Yorumlar
        </button>
        <button
          onClick={() => setActiveTab('questions')}
          className={`flex-1 py-4 text-sm font-medium transition-colors ${
            activeTab === 'questions'
              ? 'border-b-2 border-violet-500 text-violet-400 bg-violet-500/5'
              : 'text-muted-foreground hover:bg-muted/30 hover:text-foreground'
          }`}
        >
          Soru & Cevap
        </button>
      </div>

      {/* Tabs Content */}
      <div className="p-6 md:p-8 min-h-[300px]">
        {/* REVIEWS TAB */}
        {activeTab === 'reviews' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold">Ürün Değerlendirmeleri</h3>
                <p className="text-sm text-muted-foreground">Kullanıcıların bu ürün hakkındaki düşünceleri.</p>
              </div>
              <Button onClick={() => setIsReviewModalOpen(true)} className="bg-violet-600 hover:bg-violet-700">
                <ThumbsUp className="h-4 w-4 mr-2" />
                Ürünü Değerlendir
              </Button>
            </div>

            {isLoadingReviews ? (
              <div className="text-center py-10 text-muted-foreground">Yorumlar yükleniyor...</div>
            ) : reviews.length === 0 ? (
              <div className="text-center py-10 rounded-lg border border-dashed border-border/50 bg-muted/20">
                <p className="text-muted-foreground">Bu ürün için henüz değerlendirme yapılmamış. İlk değerlendiren siz olun!</p>
              </div>
            ) : (
              <div className="space-y-4">
                {(reviews || []).map((review) => (
                  <div key={review.id} className="p-5 rounded-lg border border-border/50 bg-background/50 space-y-3">
                    <div className="flex justify-between items-start">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold">{review.userName}</span>
                          {review.verifiedBuyer && (
                            <Badge variant="secondary" className="bg-emerald-500/10 text-emerald-500 hover:bg-emerald-500/20 gap-1 px-1.5 py-0">
                              <CheckCircle className="h-3 w-3" />
                              <span className="text-[10px]">Doğrulanmış Alıcı</span>
                            </Badge>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <StarRating rating={review.rating} showText={false} size={14} />
                          <span className="text-xs text-muted-foreground">
                            {new Date(review.createdAt).toLocaleDateString('tr-TR')}
                          </span>
                        </div>
                      </div>
                    </div>
                    
                    <p className="text-sm leading-relaxed">{review.comment}</p>
                    
                    {review.adminReply ? (
                      <div className="mt-3 p-4 rounded-md bg-violet-500/10 border border-violet-500/20">
                        <div className="flex items-center gap-1.5 mb-1.5">
                          <MessageSquare className="h-4 w-4 text-violet-400" />
                          <span className="text-xs font-semibold text-violet-400">Satıcı Yanıtı</span>
                        </div>
                        <p className="text-sm text-muted-foreground leading-relaxed">
                          {review.adminReply}
                        </p>
                      </div>
                    ) : isAdminOrSeller && (
                      <div className="mt-2">
                        {replyingToReview === review.id ? (
                          <div className="space-y-2 mt-3 p-3 rounded-md border border-violet-500/30 bg-violet-500/5">
                            <Textarea
                              placeholder="Yanıtlınızı yazın..."
                              value={reviewReplyText}
                              onChange={(e) => setReviewReplyText(e.target.value)}
                              className="min-h-[60px]"
                            />
                            <div className="flex justify-end gap-2">
                              <Button variant="ghost" size="sm" onClick={() => setReplyingToReview(null)}>
                                İptal
                              </Button>
                              <Button 
                                size="sm" 
                                className="bg-violet-600 hover:bg-violet-700"
                                disabled={replyToReviewMutation.isPending}
                                onClick={() => replyToReviewMutation.mutate({ reviewId: review.id, text: reviewReplyText })}
                              >
                                {replyToReviewMutation.isPending ? 'Gönderiliyor...' : 'Yanıtla'}
                              </Button>
                            </div>
                          </div>
                        ) : (
                          <Button variant="outline" size="sm" className="text-xs h-7" onClick={() => setReplyingToReview(review.id)}>
                            Yanıtla
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* QUESTIONS TAB */}
        {activeTab === 'questions' && (
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-semibold">Soru & Cevap</h3>
              <p className="text-sm text-muted-foreground">Ürün hakkında merak ettiklerinizi satıcıya sorun.</p>
            </div>

            {/* Ask Question Input */}
            <div className="p-4 rounded-lg border border-border/50 bg-background/50 flex flex-col sm:flex-row gap-3">
              <Textarea 
                placeholder="Ürünle ilgili sorunuzu buraya yazın..."
                className="resize-none min-h-[60px] flex-1"
                value={newQuestion}
                onChange={(e) => setNewQuestion(e.target.value)}
              />
              <Button 
                onClick={handleAskQuestion} 
                disabled={questionMutation.isPending}
                className="sm:h-auto bg-violet-600 hover:bg-violet-700"
              >
                {questionMutation.isPending ? 'Gönderiliyor...' : (
                  <>
                    <Send className="h-4 w-4 mr-2" />
                    Soru Sor
                  </>
                )}
              </Button>
            </div>

            {/* Questions List */}
            {isLoadingQuestions ? (
              <div className="text-center py-10 text-muted-foreground">Sorular yükleniyor...</div>
            ) : questions.length === 0 ? (
              <div className="text-center py-10 rounded-lg border border-dashed border-border/50 bg-muted/20">
                <p className="text-muted-foreground">Henüz soru sorulmamış. İlk soruyu siz sorun!</p>
              </div>
            ) : (
              <div className="space-y-4">
                {(questions || []).map((q) => (
                  <div key={q.id} className="p-5 rounded-lg border border-border/50 bg-background/50 space-y-3">
                    {/* User and Date */}
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-sm">{q.userName}</span>
                      <span className="text-xs text-muted-foreground">
                        {new Date(q.createdAt).toLocaleDateString('tr-TR')}
                      </span>
                    </div>
                    
                    {/* Question Text */}
                    <p className="text-sm font-medium text-foreground/90">{q.comment}</p>

                    {/* Admin Reply or Empty State */}
                    {q.adminReply ? (
                      <div className="mt-3 p-4 rounded-md bg-slate-900/40 border border-violet-500/20">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-1.5">
                            <MessageSquare className="h-4 w-4 text-violet-400" />
                            <span className="text-xs font-semibold text-violet-400">Satıcı Yanıtı</span>
                          </div>
                          <span className="text-[10px] text-muted-foreground">
                            {q.adminReply.repliedBy} • {new Date(q.adminReply.repliedAt).toLocaleDateString('tr-TR')}
                          </span>
                        </div>
                        <p className="text-sm text-muted-foreground leading-relaxed">
                          {q.adminReply.replyText}
                        </p>
                      </div>
                    ) : (
                      <div className="mt-2 flex flex-col gap-2">
                        <div className="text-xs text-muted-foreground italic">
                          Henüz yanıtlanmadı.
                        </div>
                        {isAdminOrSeller && (
                          replyingToQuestion === q.id ? (
                            <div className="space-y-2 mt-2 p-3 rounded-md border border-violet-500/30 bg-violet-500/5">
                              <Textarea
                                placeholder="Yanıtlınızı yazın..."
                                value={questionReplyText}
                                onChange={(e) => setQuestionReplyText(e.target.value)}
                                className="min-h-[60px]"
                              />
                              <div className="flex justify-end gap-2">
                                <Button variant="ghost" size="sm" onClick={() => setReplyingToQuestion(null)}>
                                  İptal
                                </Button>
                                <Button 
                                  size="sm" 
                                  className="bg-violet-600 hover:bg-violet-700"
                                  disabled={replyToQuestionMutation.isPending}
                                  onClick={() => replyToQuestionMutation.mutate({ questionId: q.id, text: questionReplyText })}
                                >
                                  {replyToQuestionMutation.isPending ? 'Gönderiliyor...' : 'Yanıtla'}
                                </Button>
                              </div>
                            </div>
                          ) : (
                            <div>
                              <Button variant="outline" size="sm" className="text-xs h-7" onClick={() => setReplyingToQuestion(q.id)}>
                                Yanıtla
                              </Button>
                            </div>
                          )
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <ReviewModal 
        productId={productId}
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        onSuccess={handleReviewSuccess}
      />
    </div>
  );
}
