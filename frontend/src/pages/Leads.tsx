import LeadList from '../components/LeadList';

export default function Leads() {
  return (
    <div style={{ padding: '2rem 1.5rem', background: 'var(--bg)', minHeight: 'calc(100vh - 70px)' }}>
      <LeadList />
    </div>
  );
}
