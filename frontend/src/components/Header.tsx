import { Link, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import styles from './Header.module.css';
import { healthApi } from '../services/api';

export default function Header() {
  const location = useLocation();
  const [status, setStatus] = useState<'up' | 'down' | 'checking'>('checking');

  useEffect(() => {
    healthApi.check()
      .then(() => setStatus('up'))
      .catch(() => setStatus('down'));
    const id = setInterval(() => {
      healthApi.check().then(() => setStatus('up')).catch(() => setStatus('down'));
    }, 30000);
    return () => clearInterval(id);
  }, []);

  return (
    <header className={styles.header}>
      <nav className={styles.nav}>
        <Link to="/" className={styles.logo}>
          <div className={styles.logoIcon}>🏢</div>
          <span>Sky Heights</span>
        </Link>
        <ul className={styles.navLinks}>
          <li><Link to="/" className={location.pathname === '/' ? styles.active : ''}>Voice Agent</Link></li>
          <li><Link to="/leads" className={location.pathname === '/leads' ? styles.active : ''}>Leads</Link></li>
          <li>
            <div className={styles.status}>
              <div className={`${styles.statusDot} ${status === 'down' ? styles.down : status === 'checking' ? styles.loading : ''}`} />
              {status === 'up' ? 'System UP' : status === 'checking' ? 'Checking...' : 'Offline'}
            </div>
          </li>
        </ul>
      </nav>
    </header>
  );
}
