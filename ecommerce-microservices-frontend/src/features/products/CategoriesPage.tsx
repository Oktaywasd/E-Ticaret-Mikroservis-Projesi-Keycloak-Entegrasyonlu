import { Link } from 'react-router-dom';
import { Package, ArrowRight } from 'lucide-react';
import { useCategories } from './useProductQueries';
import { Button } from '@/components/ui/button';

export function CategoriesPage() {
  const { data: categories, isLoading } = useCategories();

  return (
    <div className="container mx-auto px-4 py-12 min-h-[calc(100vh-4rem)]">
      <div className="flex flex-col gap-6 mb-8">
        <div>
          <h1 className="text-3xl md:text-4xl font-bold tracking-tight mb-2">Tüm Kategoriler</h1>
          <p className="text-muted-foreground text-lg max-w-2xl">
            Mağazamızdaki tüm ürün kategorilerini keşfedin ve aradığınız ürünlere kolayca ulaşın.
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {Array.from({ length: 15 }).map((_, i) => (
            <div key={i} className="h-32 rounded-xl bg-muted animate-pulse" />
          ))}
        </div>
      ) : categories && categories.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/products?categoryId=${cat.id}`}
              className="group relative flex flex-col items-center justify-center gap-3 rounded-xl border border-border/50 bg-card p-6 text-center hover:border-violet-500/50 hover:bg-violet-500/5 hover:shadow-lg hover:shadow-violet-500/10 transition-all duration-300"
            >
              <div className="rounded-full bg-muted p-4 group-hover:bg-violet-500/20 group-hover:scale-110 transition-all duration-300">
                <Package className="h-6 w-6 text-muted-foreground group-hover:text-violet-500 transition-colors" />
              </div>
              <span className="font-semibold text-foreground group-hover:text-violet-500 transition-colors line-clamp-2">
                {cat.name}
              </span>
              
              <div className="absolute bottom-3 right-3 opacity-0 translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300">
                <ArrowRight className="h-4 w-4 text-violet-500" />
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-20 text-center border rounded-xl bg-muted/20 border-dashed">
          <Package className="h-16 w-16 text-muted-foreground/30 mb-4" />
          <h2 className="text-xl font-semibold mb-2">Kategori Bulunamadı</h2>
          <p className="text-muted-foreground mb-6 max-w-sm">
            Şu anda sistemde ekli herhangi bir kategori bulunmuyor.
          </p>
          <Button asChild>
            <Link to="/products">Tüm Ürünlere Git</Link>
          </Button>
        </div>
      )}
    </div>
  );
}
