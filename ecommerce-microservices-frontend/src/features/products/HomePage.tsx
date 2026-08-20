import { Link } from 'react-router-dom';
import { ArrowRight, Zap, Shield, Star, Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ProductCard } from './ProductCard';
import { ProductCardSkeleton } from '@/components/ui/skeleton';
import { useProducts, useCategories } from './useProductQueries';

export function HomePage() {
  const { data: featuredData, isLoading: loadingProducts } = useProducts({
    page: 0,
    size: 8,
    sort: 'createdAt,desc',
  });

  const { data: categories, isLoading: loadingCategories } = useCategories();

  return (
    <div className="flex flex-col">
      {/* ── Hero ─────────────────────────────────────────── */}
      <section className="relative overflow-hidden bg-gradient-to-br from-slate-950 via-violet-950 to-slate-950 py-24 px-4">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-violet-600/20 via-transparent to-transparent" />
        {/* Decorative circles */}
        <div className="absolute -top-40 -right-40 h-96 w-96 rounded-full bg-violet-600/10 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-indigo-600/10 blur-3xl" />

        <div className="container mx-auto relative z-10 text-center space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-violet-500/30 bg-violet-500/10 px-4 py-1.5 text-sm text-violet-300">
            <Zap className="h-3.5 w-3.5" />
            Yeni Sezon Ürünleri Geldi
          </div>
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight text-white">
            Alışverişin
            <br />
            <span className="bg-gradient-to-r from-violet-400 to-indigo-400 bg-clip-text text-transparent">
              Yeni Adresi
            </span>
          </h1>
          <p className="mx-auto max-w-xl text-lg text-slate-400">
            Binlerce ürün, güvenli ödeme ve hızlı kargo seçenekleriyle keyifli alışveriş deneyimi.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <Button
              id="hero-cta-button"
              size="lg"
              className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 shadow-lg shadow-violet-500/30"
              asChild
            >
              <Link to="/products">
                Alışverişe Başla
                <ArrowRight className="h-4 w-4 ml-2" />
              </Link>
            </Button>
            
            <Button
              size="lg"
              className="bg-gradient-to-r from-rose-500 to-orange-500 hover:from-rose-600 hover:to-orange-600 text-white shadow-lg shadow-rose-500/30"
              asChild
            >
              <Link to="/reels">
                🎬 Trend Reels İzle
              </Link>
            </Button>

            <Button 
              size="lg" 
              variant="outline" 
              className="border-white/20 text-white hover:bg-white/10" 
              onClick={() => document.getElementById('categories')?.scrollIntoView({ behavior: 'smooth' })}
            >
              Kategoriler
            </Button>
          </div>
        </div>
      </section>

      {/* ── Features ─────────────────────────────────────── */}
      <section className="py-12 px-4 bg-background border-b border-border/40">
        <div className="container mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { icon: <Zap className="h-5 w-5 text-violet-400" />, title: 'Hızlı Teslimat', desc: 'Siparişleriniz 24 saat içinde kapınızda.' },
              { icon: <Shield className="h-5 w-5 text-emerald-400" />, title: 'Güvenli Ödeme', desc: 'SSL şifrelemeli güvenli ödeme altyapısı.' },
              { icon: <Star className="h-5 w-5 text-amber-400" />, title: 'Kalite Garantisi', desc: '30 gün içinde ücretsiz iade hakkı.' },
            ].map((f) => (
              <div
                key={f.title}
                className="flex items-center gap-4 rounded-xl border border-border/50 bg-card p-5 hover:border-violet-500/20 transition-colors"
              >
                <div className="rounded-lg bg-muted p-3 border border-border/50 shrink-0">{f.icon}</div>
                <div>
                  <h3 className="font-semibold text-sm">{f.title}</h3>
                  <p className="text-xs text-muted-foreground">{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Categories ─────────────────────────────────── */}
      {(loadingCategories || (categories && categories.length > 0)) && (
        <section id="categories" className="py-14 px-4 bg-background">
          <div className="container mx-auto space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-bold">Kategoriler</h2>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/categories">Tümünü Gör <ArrowRight className="h-4 w-4 ml-1" /></Link>
              </Button>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              {loadingCategories
                ? Array.from({ length: 6 }).map((_, i) => (
                    <div key={i} className="h-20 rounded-xl bg-muted animate-pulse" />
                  ))
                : categories?.slice(0, 6).map((cat) => (
                    <Link
                      key={cat.id}
                      to={`/products?categoryId=${cat.id}`}
                      className="flex flex-col items-center justify-center gap-2 rounded-xl border border-border/50 bg-card p-4 text-center hover:border-violet-500/40 hover:bg-violet-500/5 transition-all group"
                    >
                      <div className="rounded-full bg-muted p-3 group-hover:bg-violet-500/20 transition-colors">
                        <Package className="h-5 w-5 text-muted-foreground group-hover:text-violet-400 transition-colors" />
                      </div>
                      <span className="text-xs font-medium leading-tight line-clamp-2">{cat.name}</span>
                    </Link>
                  ))}
            </div>
          </div>
        </section>
      )}

      {/* ── Featured Products ─────────────────────────── */}
      <section className="py-14 px-4 bg-muted/10">
        <div className="container mx-auto space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold">Öne Çıkan Ürünler</h2>
              <p className="text-sm text-muted-foreground">En yeni ürünlerimizi keşfedin</p>
            </div>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/products">
                Tümünü Gör <ArrowRight className="h-4 w-4 ml-1" />
              </Link>
            </Button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {loadingProducts
              ? Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)
              : featuredData?.content.length
              ? featuredData.content.map((p) => <ProductCard key={p.id} product={p} />)
              : (
                <div className="col-span-full flex flex-col items-center gap-3 py-12 text-center">
                  <Package className="h-12 w-12 text-muted-foreground/30" />
                  <p className="text-muted-foreground text-sm">Henüz ürün eklenmemiş.</p>
                </div>
              )}
          </div>
        </div>
      </section>
    </div>
  );
}
