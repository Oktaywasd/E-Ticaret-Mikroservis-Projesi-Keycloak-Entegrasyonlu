import { Link } from 'react-router-dom';
import { Package, Globe, MessageSquare, Mail } from 'lucide-react';

export function Footer() {
  return (
    <footer className="border-t border-border/40 bg-background/80 mt-auto">
      <div className="container mx-auto px-4 py-10">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-4">
          {/* Brand */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600">
                <Package className="h-4 w-4 text-white" />
              </div>
              <span className="font-bold text-lg bg-gradient-to-r from-violet-400 to-indigo-400 bg-clip-text text-transparent">
                EShop
              </span>
            </div>
            <p className="text-sm text-muted-foreground">
              Modern e-ticaret deneyimi. Güvenli, hızlı ve kullanıcı dostu alışveriş platformu.
            </p>
          </div>

          {/* Kurumsal */}
          <div className="space-y-3">
            <h4 className="font-semibold text-sm">Kurumsal</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/about" className="hover:text-foreground transition-colors">Hakkımızda</Link></li>
              <li><Link to="/contact" className="hover:text-foreground transition-colors">İletişim</Link></li>
              <li><Link to="/careers" className="hover:text-foreground transition-colors">Kariyer</Link></li>
            </ul>
          </div>

          {/* Yardım */}
          <div className="space-y-3">
            <h4 className="font-semibold text-sm">Yardım</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/faq" className="hover:text-foreground transition-colors">SSS</Link></li>
              <li><Link to="/returns" className="hover:text-foreground transition-colors">İade & Değişim</Link></li>
              <li><Link to="/shipping" className="hover:text-foreground transition-colors">Kargo Bilgisi</Link></li>
            </ul>
          </div>

          {/* Yasal */}
          <div className="space-y-3">
            <h4 className="font-semibold text-sm">Yasal</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/privacy" className="hover:text-foreground transition-colors">Gizlilik Politikası</Link></li>
              <li><Link to="/terms" className="hover:text-foreground transition-colors">Kullanım Koşulları</Link></li>
              <li><Link to="/cookies" className="hover:text-foreground transition-colors">Çerez Politikası</Link></li>
            </ul>
          </div>
        </div>

        <div className="mt-8 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-border/40 pt-6">
          <p className="text-xs text-muted-foreground">
            © {new Date().getFullYear()} EShop. Tüm hakları saklıdır.
          </p>
          <div className="flex items-center gap-4">
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <Globe className="h-4 w-4" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <MessageSquare className="h-4 w-4" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <Mail className="h-4 w-4" />
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
