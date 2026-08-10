import React from 'react';

const CRON_OPTIONS = [
  { label: 'Every 15 minutes', value: '0 0/15 * * * ?' },
  { label: 'Every 30 minutes', value: '0 0/30 * * * ?' },
  { label: 'Every hour', value: '0 0 * * * ?' },
  { label: 'Every 4 hours', value: '0 0 0/4 * * ?' },
  { label: 'Every day (at 02:00 UTC)', value: '0 0 2 * * ?' },
  { label: 'Every week (Sunday at 02:00 UTC)', value: '0 0 2 ? * SUN' },
  { label: 'Every month (1st at 02:00 UTC)', value: '0 0 2 1 * ?' },
  { label: 'Custom', value: 'CUSTOM' }
];

interface CronPickerProps {
  value: string;
  onChange: (value: string) => void;
}

export default function CronPicker({ value, onChange }: CronPickerProps) {
  const isPredefined = CRON_OPTIONS.some(opt => opt.value === value && opt.value !== 'CUSTOM');
  const selectValue = isPredefined ? value : 'CUSTOM';

  const handleSelectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    if (val === 'CUSTOM') {
      onChange('');
    } else {
      onChange(val);
    }
  };

  const handleCustomChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
      <select 
        value={selectValue} 
        onChange={handleSelectChange}
        style={{
          width: '100%',
          padding: '0.75rem',
          background: 'var(--color-surface-hover)',
          border: '1px solid var(--color-border)',
          borderRadius: '6px',
          color: 'var(--color-text)',
          fontSize: '1rem',
          outline: 'none',
          transition: 'border-color 0.2s',
          cursor: 'pointer',
          appearance: 'none', // Remove default arrow in some browsers
          backgroundImage: 'url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23cccccc%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.4-12.8z%22%2F%3E%3C%2Fsvg%3E")',
          backgroundRepeat: 'no-repeat',
          backgroundPosition: 'right 0.75rem top 50%',
          backgroundSize: '0.65rem auto'
        }}
      >
        {CRON_OPTIONS.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      
      {selectValue === 'CUSTOM' && (
        <input 
          type="text" 
          value={value} 
          onChange={handleCustomChange} 
          placeholder="e.g. 0 0/5 * * * ?"
          style={{ fontFamily: 'var(--font-mono)' }}
          required
        />
      )}
    </div>
  );
}
