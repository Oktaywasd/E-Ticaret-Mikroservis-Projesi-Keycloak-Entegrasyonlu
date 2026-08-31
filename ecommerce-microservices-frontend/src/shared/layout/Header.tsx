import { useNavigate, Link } from 'react-router-dom';
import {
  ShoppingCart,
  User,
  LogOut,
  Settings,
  Package,
  LayoutDashboard,
  Menu,
  X,
  ChevronDown,
  Search,
  Image as ImageIcon
} from 'lucide-react';
import { useState, useEffect, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAppAuth } from '@/hooks/useAppAuth';
import { Badge } from '@/components/ui/badge';
import { useCartStore } from '@/features/cart/cartStore';
import { redirectToRegister } from '@/lib/auth/register';
import { useDebounce } from '@/hooks/useDebounce';
import { getSuggestions } from '@/features/products/productService';
import type { ProductSuggestion } from '@/features/products/types';

export function Header() {
  const auth = useAppAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const cartCount = useCartStore((s) => s.items.reduce((acc, i) => acc + i.quantity, 0));

  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 300);
  const [suggestions, setSuggestions] = useState<ProductSuggestion[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (debouncedSearch.trim().length >= 2) {
      setIsSearching(true);
      getSuggestions(debouncedSearch.trim())
        .then(res => setSuggestions(res))
        .catch(err => console.error(err))
        .finally(() => setIsSearching(false));
      setShowDropdown(true);
    } else {
      setSuggestions([]);
      setShowDropdown(false);
    }
  }, [debouncedSearch]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearchSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (searchQuery.trim()) {
      setShowDropdown(false);
      navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleLogin = () => auth.signinRedirect();
  const handleLogout = () => auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin });

  const handleRegister = () => {
    redirectToRegister(auth).catch((err) => console.error("Register redirect failed:", err));
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto flex h-16 items-center gap-4 px-4">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 shadow-lg shadow-violet-500/30">
            <Package className="h-4 w-4 text-white" />
          </div>
          <span className="hidden font-bold text-lg bg-gradient-to-r from-violet-400 to-indigo-400 bg-clip-text text-transparent sm:block">
            EShop
          </span>
        </Link>



        {/* Nav Links */}
        <nav className="hidden md:flex items-center gap-1">
          <Button variant="ghost" size="sm" asChild>
            <Link to="/products">Ürünler</Link>
          </Button>
          <Button variant="ghost" size="sm" asChild className="group relative">
            <Link to="/reels" data-testid="reels-nav-link">
              Reels
              <span className="ml-1.5 inline-flex items-center rounded-md bg-rose-500/10 px-1.5 py-0.5 text-[10px] font-medium text-rose-500 ring-1 ring-inset ring-rose-500/20 group-hover:bg-rose-500/20">
                Yeni
              </span>
            </Link>
          </Button>
          {auth.isAdminOrSeller && (
            <Button variant="ghost" size="sm" asChild>
              <Link to="/admin/orders">Panel</Link>
            </Button>
          )}
        </nav>

        {/* Search Bar */}
        <div className="hidden sm:block flex-1 max-w-md mx-auto" ref={searchRef}>
          <form onSubmit={handleSearchSubmit} className="relative group">
            <button 
              type="submit" 
              className="absolute inset-y-0 left-0 pl-3 flex items-center text-muted-foreground hover:text-violet-500 group-focus-within:text-violet-500 transition-colors"
            >
              <Search className="h-4 w-4" />
            </button>
            <Input
              type="text"
              data-testid="search-input"
              placeholder="Ürün veya marka ara..."
              className="pl-9 bg-muted/50 border-transparent focus-visible:bg-background focus-visible:border-violet-500/50 focus-visible:ring-violet-500/20 h-9 w-full"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                if (!showDropdown) setShowDropdown(true);
              }}
              onFocus={() => {
                if (searchQuery.trim().length >= 2) setShowDropdown(true);
              }}
            />
            
            {/* Dropdown */}
            {showDropdown && searchQuery.trim().length >= 2 && (
              <div className="absolute top-full left-0 right-0 mt-2 bg-popover border border-border/50 rounded-xl shadow-xl shadow-black/20 overflow-hidden z-50">
                {isSearching ? (
                  <div className="p-4 text-center text-sm text-muted-foreground flex items-center justify-center gap-2">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-violet-500 border-t-transparent" />
                    Aranıyor...
                  </div>
                ) : suggestions.length > 0 ? (
                  <ul className="max-h-[300px] overflow-auto py-1">
                    {suggestions.map((item) => (
                      <li key={item.id}>
                        <Link
                          to={`/products/${item.id}`}
                          onClick={() => {
                            setShowDropdown(false);
                            setSearchQuery('');
                          }}
                          className="flex items-center gap-3 px-4 py-2.5 hover:bg-muted/50 transition-colors"
                        >
                          <div className="h-10 w-10 rounded-md bg-muted flex items-center justify-center overflow-hidden shrink-0 border border-border/50">
                            {item.imageUrl ? (
                              <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
                            ) : (
                              <ImageIcon className="h-4 w-4 text-muted-foreground/50" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium truncate text-foreground">{item.name}</p>
                            {item.brand && (
                              <p className="text-xs text-muted-foreground truncate">{item.brand}</p>
                            )}
                          </div>
                          <div className="text-sm font-semibold text-violet-400 shrink-0">
                            {new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(item.price)}
                          </div>
                        </Link>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="p-4 text-center text-sm text-muted-foreground italic">
                    Eşleşen ürün bulunamadı.
                  </div>
                )}
              </div>
            )}
          </form>
        </div>

        <div className="flex items-center gap-2 ml-auto">
          {/* Cart */}
          <Button variant="ghost" size="icon" asChild className="relative">
            <Link to="/cart" id="cart-button" data-testid="cart-button">
              <ShoppingCart className="h-5 w-5" />
              {cartCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-violet-600 text-[10px] font-bold text-white" data-testid="cart-badge">
                  {cartCount > 99 ? '99+' : cartCount}
                </span>
              )}
            </Link>
          </Button>

          {/* User Menu */}
          {auth.isAuthenticated ? (
            <div className="relative">
              <Button
                id="user-menu-button"
                variant="ghost"
                size="sm"
                className="flex items-center gap-2"
                onClick={() => setUserMenuOpen((v) => !v)}
              >
                <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-br from-violet-600 to-indigo-600 text-xs font-bold text-white">
                  {auth.displayName.charAt(0).toUpperCase()}
                </div>
                <span className="hidden sm:block text-sm max-w-[100px] truncate">
                  {auth.displayName}
                </span>
                <ChevronDown className="h-3 w-3 text-muted-foreground" />
              </Button>

              {userMenuOpen && (
                <>
                  <div
                    className="fixed inset-0 z-10"
                    onClick={() => setUserMenuOpen(false)}
                  />
                  <div className="absolute right-0 top-full z-20 mt-2 w-52 rounded-xl border border-border/50 bg-popover shadow-xl shadow-black/20 py-1">
                    <div className="px-3 py-2 border-b border-border/50">
                      <p className="text-sm font-semibold truncate">{auth.displayName}</p>
                      <p className="text-xs text-muted-foreground truncate">{auth.email}</p>
                    </div>
                    <Link
                      to="/profile"
                      className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-accent transition-colors"
                      onClick={() => setUserMenuOpen(false)}
                    >
                      <User className="h-4 w-4" />
                      Hesabım
                    </Link>
                    <Link
                      to="/orders"
                      className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-accent transition-colors"
                      onClick={() => setUserMenuOpen(false)}
                    >
                      <Package className="h-4 w-4" />
                      Siparişlerim
                    </Link>
                    {auth.isAdminOrSeller && (
                      <Link
                        to="/admin/orders"
                        className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-accent transition-colors"
                        onClick={() => setUserMenuOpen(false)}
                      >
                        <LayoutDashboard className="h-4 w-4" />
                        Yönetim Paneli
                      </Link>
                    )}
                    <div className="border-t border-border/50 mt-1 pt-1">
                      <button
                        id="logout-button"
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2 px-3 py-2 text-sm text-destructive hover:bg-destructive/10 transition-colors"
                      >
                        <LogOut className="h-4 w-4" />
                        Çıkış Yap
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Button id="login-button" data-testid="login-button" variant="ghost" size="sm" onClick={handleLogin}>
                Giriş Yap
              </Button>
              <Button
                id="register-button"
                size="sm"
                className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 shadow-lg shadow-violet-500/25"
                onClick={handleRegister}
              >
                Kayıt Ol
              </Button>
            </div>
          )}

          {/* Mobile menu toggle */}
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setMobileMenuOpen((v) => !v)}
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </Button>
        </div>
      </div>

      {/* Mobile Search + Nav */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-border/40 px-4 py-3 space-y-3 bg-background">

          <nav className="flex flex-col gap-1">
            <Button variant="ghost" className="justify-start" asChild>
              <Link to="/products" onClick={() => setMobileMenuOpen(false)}>
                Ürünler
              </Link>
            </Button>
            <Button variant="ghost" className="justify-start text-rose-500 hover:text-rose-600 hover:bg-rose-500/10" asChild>
              <Link to="/reels" onClick={() => setMobileMenuOpen(false)} data-testid="reels-nav-link">
                Reels <Badge variant="secondary" className="ml-2 bg-rose-500/20 text-rose-600 hover:bg-rose-500/30">Yeni</Badge>
              </Link>
            </Button>
            {auth.isAdminOrSeller && (
              <Button variant="ghost" className="justify-start" asChild>
                <Link to="/admin/orders" onClick={() => setMobileMenuOpen(false)}>
                  <LayoutDashboard className="h-4 w-4 mr-2" />
                  Yönetim Paneli
                </Link>
              </Button>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
