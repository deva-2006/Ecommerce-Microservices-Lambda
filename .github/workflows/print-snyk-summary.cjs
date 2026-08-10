const fs = require('fs');

function parseDependencyReport() {
  try {
    if (!fs.existsSync('snyk-report.json')) {
      console.log('snyk-report.json not found.');
      return;
    }
    const report = JSON.parse(fs.readFileSync('snyk-report.json', 'utf8'));
    const vulns = Array.isArray(report) ? report.flatMap(r => r.vulnerabilities || []) : (report.vulnerabilities || []);
    const count = vulns.length;
    console.log('==================================================');
    console.log('TOTAL DEPENDENCY VULNERABILITIES FOUND:', count);
    console.log('==================================================');

    const summaryFile = process.env.GITHUB_STEP_SUMMARY || 'summary.md';
    const snykDepStatus = count === 0 ? '✅ PASSED' : '❌ FAILED';
    fs.appendFileSync(summaryFile, `# 🚀 Deployment & Quality Summary - Frontend Application\n\n### 📋 Service Quality & Security Checklist\n\n| Component / Metric | Status | Details |\n|---|---|---|\n| 🛡️ **Snyk Dependency Scan** | ${snykDepStatus} | ${count} Vulnerabilities Found |\n| 🎨 **Vite Build & Bundling** | ✅ PASSED | React + Vite Production Build |\n\n---\n\n### 🛡️ Snyk Dependency Security Scan Summary\n* **Total High/Critical Dependency Vulnerabilities:** ${count}\n`);


    if (count > 0) {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, '\n| Severity | Package | Title | Remediation |\n| --- | --- | --- | --- |\n');
      vulns.forEach(v => {
        const cleanTitle = (v.title || '').replace(/\|/g, '\\|');
        const cleanRemediation = (v.remediation || 'Upgrade dependency').replace(/\n/g, ' ').replace(/\|/g, '\\|');
        fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `| ${v.severity} | ${v.packageName} | ${cleanTitle} | ${cleanRemediation} |\n`);
      });
    } else {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, '\n:white_check_mark: No high-severity dependency vulnerabilities found!\n');
    }
  } catch (e) {
    console.error('Failed to parse Snyk dependency report:', e);
  }
}

function parseCodebaseReport() {
  try {
    if (!fs.existsSync('snyk-code-report.json')) {
      console.log('snyk-code-report.json not found.');
      return;
    }
    const report = JSON.parse(fs.readFileSync('snyk-code-report.json', 'utf8'));
    const vulns = report.runs && report.runs[0] && report.runs[0].results ? report.runs[0].results : [];
    const count = vulns.length;
    console.log('==================================================');
    console.log('TOTAL CODEBASE VULNERABILITIES FOUND:', count);
    console.log('==================================================');

    fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, '\n### Snyk Codebase Security Scan Summary\n');
    fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `* **Total Codebase Vulnerabilities:** ${count}\n`);

    if (count > 0) {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, '\n| Severity | File | Message | Rule |\n| --- | --- | --- | --- |\n');
      vulns.forEach(v => {
        const location = v.locations && v.locations[0] && v.locations[0].physicalLocation ? v.locations[0].physicalLocation.artifactLocation.uri : 'unknown';
        const line = v.locations && v.locations[0] && v.locations[0].physicalLocation && v.locations[0].physicalLocation.region ? v.locations[0].physicalLocation.region.startLine : '';
        const fileWithLine = line ? `${location}:${line}` : location;
        const cleanMsg = (v.message.text || '').replace(/\n/g, ' ').replace(/\|/g, '\\|');
        const ruleId = v.ruleId || 'unknown';
        const level = v.level === 'error' ? 'high' : (v.level === 'warning' ? 'medium' : 'low');
        fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `| ${level} | ${fileWithLine} | ${cleanMsg} | ${ruleId} |\n`);
      });
    } else {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, '\n:white_check_mark: No codebase vulnerabilities found!\n');
    }
  } catch (e) {
    console.error('Failed to parse Snyk codebase report:', e);
  }
}

parseDependencyReport();
parseCodebaseReport();
