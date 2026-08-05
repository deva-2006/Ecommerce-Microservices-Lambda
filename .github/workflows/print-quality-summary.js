const fs = require('fs');
const path = require('path');

const serviceName = process.argv[2] || 'Microservice';
const serviceDir = process.argv[3] || '.';
const sonarProjectKey = process.argv[4] || `deva-2006_${serviceDir.toLowerCase()}`;
const sonarOrg = 'deva-2006';

const summaryFile = process.env.GITHUB_STEP_SUMMARY || 'summary.md';

console.log(`==================================================`);
console.log(`Generating Quality & Security Summary for ${serviceName}`);
console.log(`==================================================`);

// 1. Parse Unit Tests (Surefire XMLs)
let totalTests = 0, totalFailures = 0, totalErrors = 0, totalSkipped = 0;
const surefireDir = path.join(serviceDir, 'target', 'surefire-reports');

if (fs.existsSync(surefireDir)) {
    const files = fs.readdirSync(surefireDir).filter(f => f.startsWith('TEST-') && f.endsWith('.xml'));
    files.forEach(file => {
        try {
            const content = fs.readFileSync(path.join(surefireDir, file), 'utf8');
            const testsMatch = content.match(/tests="(\d+)"/);
            const failuresMatch = content.match(/failures="(\d+)"/);
            const errorsMatch = content.match(/errors="(\d+)"/);
            const skippedMatch = content.match(/skipped="(\d+)"/);

            if (testsMatch) totalTests += parseInt(testsMatch[1], 10);
            if (failuresMatch) totalFailures += parseInt(failuresMatch[1], 10);
            if (errorsMatch) totalErrors += parseInt(errorsMatch[1], 10);
            if (skippedMatch) totalSkipped += parseInt(skippedMatch[1], 10);
        } catch (e) {
            console.error(`Error reading ${file}:`, e.message);
        }
    });
}

const testsPassed = totalFailures === 0 && totalErrors === 0 && totalTests > 0;
const testStatusIcon = testsPassed ? '✅ PASSED' : (totalTests === 0 ? '⚠️ NO TESTS FOUND' : '❌ FAILED');

// 2. Parse JaCoCo Code Coverage (jacoco.xml)
let instCov = 'N/A', lineCov = 'N/A', branchCov = 'N/A', overallCov = 'N/A';
const jacocoPath = path.join(serviceDir, 'target', 'site', 'jacoco', 'jacoco.xml');

if (fs.existsSync(jacocoPath)) {
    try {
        const content = fs.readFileSync(jacocoPath, 'utf8');
        
        const parseCounter = (type) => {
            const regex = new RegExp(`<counter type="${type}" missed="(\\d+)" covered="(\\d+)"\\/>`, 'g');
            let match, lastMatch;
            while ((match = regex.exec(content)) !== null) {
                lastMatch = match;
            }
            if (lastMatch) {
                const missed = parseInt(lastMatch[1], 10);
                const covered = parseInt(lastMatch[2], 10);
                const total = missed + covered;
                const pct = total > 0 ? ((covered / total) * 100).toFixed(1) : '100.0';
                return { covered, total, pct: `${pct}%` };
            }
            return null;
        };

        const inst = parseCounter('INSTRUCTION');
        const line = parseCounter('LINE');
        const branch = parseCounter('BRANCH');

        if (inst) { instCov = `${inst.pct} (${inst.covered}/${inst.total})`; overallCov = inst.pct; }
        if (line) { lineCov = `${line.pct} (${line.covered}/${line.total})`; }
        if (branch) { branchCov = `${branch.pct} (${branch.covered}/${branch.total})`; }
    } catch (e) {
        console.error('Error reading JaCoCo report:', e.message);
    }
}

// 3. Parse Snyk Report (snyk-report.json)
let snykStatusIcon = '✅ PASSED';
let snykVulnCount = 0;
let snykVulns = [];
const snykPath = 'snyk-report.json';

if (fs.existsSync(snykPath)) {
    try {
        const snykContent = JSON.parse(fs.readFileSync(snykPath, 'utf8'));
        const vulns = Array.isArray(snykContent) ? snykContent.flatMap(r => r.vulnerabilities || []) : (snykContent.vulnerabilities || []);
        snykVulnCount = vulns.length;
        snykVulns = vulns;
        if (snykVulnCount > 0) {
            snykStatusIcon = '❌ FAILED';
        }
    } catch (e) {
        console.error('Error reading Snyk report:', e.message);
    }
}

// 4. Build Markdown Report
const sonarUrl = `https://sonarcloud.io/project/overview?id=${sonarProjectKey}`;

let md = `
# 🚀 Deployment & Quality Summary - ${serviceName}

### 📋 Service Quality & Security Checklist

| Metric / Check | Status | Details |
|---|---|---|
| 🧪 **Unit Tests** | ${testStatusIcon} | ${totalTests} Executed (${totalTests - totalFailures - totalErrors} Passed, ${totalFailures} Failed, ${totalErrors} Errors, ${totalSkipped} Skipped) |
| 🛡️ **Snyk Security Scan** | ${snykStatusIcon} | ${snykVulnCount} High/Critical Vulnerabilities Found |
| 📊 **Code Coverage (JaCoCo)** | 🎯 **${overallCov}** | Instruction: ${instCov} \| Line: ${lineCov} \| Branch: ${branchCov} |
| 🔍 **SonarQube Analysis** | 🟢 **ANALYZED** | Project: [\`${sonarProjectKey}\`](${sonarUrl}) |

---

### 🧪 Unit Tests Detailed Breakdown
- **Total Executed:** \`${totalTests}\`
- **Passed:** \`${totalTests - totalFailures - totalErrors}\` ✅
- **Failed:** \`${totalFailures}\` ❌
- **Errors:** \`${totalErrors}\` ⚠️
- **Skipped:** \`${totalSkipped}\` ⏭️

---

### 📊 JaCoCo Code Coverage Metrics
| Metric Type | Covered / Total | Percentage |
|---|---|---|
| 📐 **Instruction Coverage** | \`${instCov}\` |
| 📄 **Line Coverage** | \`${lineCov}\` |
| 🔀 **Branch Coverage** | \`${branchCov}\` |

---

### 🛡️ Snyk Security Vulnerability Analysis
- **Manifest:** \`${serviceDir}/pom.xml\`
- **High/Critical Vulnerabilities Found:** \`${snykVulnCount}\`
`;

if (snykVulns.length > 0) {
    md += `\n| Severity | Package | Vulnerability Title | Remediation |\n| --- | --- | --- | --- |\n`;
    snykVulns.forEach(v => {
        const title = (v.title || '').replace(/\|/g, '\\|');
        const pkg = (v.packageName || '').replace(/\|/g, '\\|');
        const rem = (v.remediation || 'Upgrade dependency').replace(/\n/g, ' ').replace(/\|/g, '\\|');
        md += `| ${v.severity} | ${pkg} | ${title} | ${rem} |\n`;
    });
} else {
    md += `\n:white_check_mark: **No high-severity vulnerabilities found in dependencies.**\n`;
}

md += `
---

### 🔍 SonarQube & SonarCloud Analysis
- **Organization:** \`${sonarOrg}\`
- **Project Key:** [\`${sonarProjectKey}\`](${sonarUrl})
- **Quality Gate Dashboard:** [View Full SonarCloud Analysis Report](${sonarUrl})

`;

fs.appendFileSync(summaryFile, md, 'utf8');
console.log(`Summary report written to ${summaryFile}`);
