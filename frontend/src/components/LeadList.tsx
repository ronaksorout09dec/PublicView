import { useEffect, useState } from 'react';
import styles from './LeadList.module.css';
import { leadApi } from '../services/api';
import type { Lead } from '../types';

export default function LeadList() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLeads = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await leadApi.getAll();
      if (res.success) {
        setLeads(res.data || []);
      } else {
        setError(res.message);
      }
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Failed to fetch leads');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeads();
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this lead?')) return;
    try {
      await leadApi.delete(id);
      setLeads(prev => prev.filter(l => l.id !== id));
    } catch (e: any) {
      alert('Delete failed: ' + (e.response?.data?.message || e.message));
    }
  };

  if (loading) {
    return <div className={styles.loading}>Loading leads... ⏳</div>;
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>📋 Captured Leads</h2>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <span className={styles.count}>{leads.length} leads</span>
          <button className={styles.btn} onClick={fetchLeads}>🔄 Refresh</button>
        </div>
      </div>

      {error && <div className={styles.error}>⚠️ {error}</div>}

      {leads.length === 0 ? (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>📭</div>
          <h3>No leads yet</h3>
          <p>Start a voice conversation to capture leads. They will appear here.</p>
        </div>
      ) : (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>ID</th>
                <th>Customer</th>
                <th>Phone</th>
                <th>Location</th>
                <th>Config</th>
                <th>Budget</th>
                <th>Purpose</th>
                <th>Timeline</th>
                <th>Summary</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {leads.map(lead => (
                <tr key={lead.id}>
                  <td>#{lead.id}</td>
                  <td><strong>{lead.customerName}</strong></td>
                  <td className={styles.phone}>{lead.phone}</td>
                  <td>{lead.location || '-'}</td>
                  <td>{lead.configuration ? <span className={`${styles.badge} ${styles.badgeConfiguration}`}>{lead.configuration}</span> : '-'}</td>
                  <td>{lead.budget || '-'}</td>
                  <td>{lead.purpose ? <span className={`${styles.badge} ${styles.badgePurpose}`}>{lead.purpose}</span> : '-'}</td>
                  <td>{lead.timeline ? <span className={`${styles.badge} ${styles.badgeTimeline}`}>{lead.timeline}</span> : '-'}</td>
                  <td className={styles.summary} title={lead.conversationSummary}>{lead.conversationSummary || '-'}</td>
                  <td style={{ whiteSpace: 'nowrap', fontSize: '0.8rem' }}>{lead.createdAt ? new Date(lead.createdAt).toLocaleDateString() : '-'}</td>
                  <td>
                    <div className={styles.actions}>
                      <button className={`${styles.btn} ${styles.btnDanger}`} onClick={() => lead.id && handleDelete(lead.id)}>🗑️</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
