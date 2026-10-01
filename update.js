const fs = require('fs');
const path = require('path');

const frontendDir = 'e:/New folder/eco-track-smart-carbon-tracker-main/frontend/src';
const rootDirs = [frontendDir];

const colorMap = {
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
      
      // Name updates
      if (content.includes('EcoTrack')) {
        content = content.replace(/EcoTrack/g, 'CarbonPulse');
        changed = true;
      }
      
      if (file === 'styles.css' || file === 'index.html') {
          if (changed) fs.writeFileSync(fullPath, content, 'utf8');
          continue;
      }
      
      if (fullPath.endsWith('.css') || fullPath.endsWith('.scss') || fullPath.endsWith('.html')) {
        for (const [hex, cssVar] of Object.entries(colorMap)) {
          const regex = new RegExp(hex, 'gi');
          if (regex.test(content)) {
            content = content.replace(regex, cssVar);
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

rootDirs.forEach(walk);
console.log('Update Complete');
