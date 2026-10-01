const fs = require('fs');
const path = require('path');

const frontendDir = 'e:/New folder/eco-track-smart-carbon-tracker-main/frontend/src/app';

const colorMap = {
  // Hex to CSS Vars for CSS/HTML
  '#0c1017': 'var(--color-background)',
  '#0f172a': 'var(--color-background)',
  '#121826': 'var(--color-card)',
  '#1e293b': 'var(--color-card)',
  '#111827': 'var(--color-card)',
  '#1f2937': 'var(--color-card)',
  '#10b981': 'var(--color-primary)',
  '#34d399': 'var(--color-primary)',
  '#059669': 'var(--color-accent)',
  '#047857': 'var(--color-accent)',
  '#f8fafc': 'var(--color-text-primary)',
  '#ffffff': 'var(--color-text-primary)',
  '#f1f5f9': 'var(--color-text-primary)',
  '#e2e8f0': 'var(--color-text-secondary)',
  '#94a3b8': 'var(--color-text-secondary)',
  '#64748b': 'var(--color-text-secondary)',
  '#475569': 'var(--color-text-muted)'
};

const hexToHexForTS = {
  '#0c1017': '#050B15',
  '#0f172a': '#050B15',
  '#121826': '#0D1829',
  '#1e293b': '#0D1829',
  '#10b981': '#00FFA3',
  '#34d399': '#00FFA3',
  '#059669': '#06B6D4',
  '#047857': '#06B6D4',
  '#f8fafc': '#E8F4F8',
  '#f1f5f9': '#E8F4F8',
  '#e2e8f0': '#8FA8C0',
  '#94a3b8': '#8FA8C0'
};

const rgbaMap = {
  '16, 185, 129': 'var(--color-primary-rgb)',
  '16,185,129': 'var(--color-primary-rgb)',
  '5, 150, 105': 'var(--color-accent-rgb)',
  '5,150,105': 'var(--color-accent-rgb)',
  '46, 125, 50': 'var(--color-primary-rgb)',
  '46,125,50': 'var(--color-primary-rgb)',
  '18, 24, 38': 'var(--color-card-rgb)',
  '18,24,38': 'var(--color-card-rgb)',
  '239, 68, 68': 'var(--color-error-rgb)',
  '239,68,68': 'var(--color-error-rgb)',
  '245, 158, 11': 'var(--color-warning-rgb)',
  '245,158,11': 'var(--color-warning-rgb)',
  '15, 23, 42': 'var(--color-background-rgb)',
  '15,23,42': 'var(--color-background-rgb)'
};

function walk(directory) {
  if (!fs.existsSync(directory)) return;
  const files = fs.readdirSync(directory);
  for (const file of files) {
    const fullPath = path.join(directory, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walk(fullPath);
    } else {
      let content = fs.readFileSync(fullPath, 'utf8');
      let changed = false;
      
      // Handle TS files differently (use raw hex values)
      if (fullPath.endsWith('.ts')) {
          for (const [hex, newHex] of Object.entries(hexToHexForTS)) {
              const regex = new RegExp(hex, 'gi');
              if (regex.test(content)) {
                  content = content.replace(regex, newHex);
                  changed = true;
              }
          }
          // Also replace rgba manually in TS
          for (const [oldRgba, newCssVar] of Object.entries(rgbaMap)) {
              if (content.includes(oldRgba)) {
                  // If TS has rgba, we replace it with the actual RGB string
                  let rawRgb = '0, 255, 163';
                  if (newCssVar.includes('accent')) rawRgb = '6, 182, 212';
                  else if (newCssVar.includes('card')) rawRgb = '13, 24, 41';
                  else if (newCssVar.includes('background')) rawRgb = '5, 11, 21';
                  else if (newCssVar.includes('error')) rawRgb = '255, 77, 109';
                  else if (newCssVar.includes('warning')) rawRgb = '255, 209, 102';
                  
                  const regex = new RegExp(oldRgba.replace(/,/g, '\\s*,\\s*'), 'gi');
                  content = content.replace(regex, rawRgb);
                  changed = true;
              }
          }
      } else if (fullPath.endsWith('.css') || fullPath.endsWith('.scss') || fullPath.endsWith('.html')) {
          for (const [hex, cssVar] of Object.entries(colorMap)) {
            const regex = new RegExp(hex, 'gi');
            if (regex.test(content)) {
              content = content.replace(regex, cssVar);
              changed = true;
            }
          }
          
          for (const [oldRgba, newCssVar] of Object.entries(rgbaMap)) {
              const regex = new RegExp(oldRgba.replace(/,/g, '\\s*,\\s*'), 'gi');
              if (regex.test(content)) {
                  content = content.replace(regex, newCssVar);
                  changed = true;
              }
          }
      }
      
      if (changed) {
        fs.writeFileSync(fullPath, content, 'utf8');
      }
    }
  }
}

walk(frontendDir);
console.log('Update Complete Phase 2');
