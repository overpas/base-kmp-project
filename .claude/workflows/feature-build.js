export const meta = {
  name: 'feature-build',
  description: 'Implement, verify, review and demo an approved feature factory plan without human input',
  whenToUse: 'Started by /feature-factory after the plan. Run again with the same args to continue a stopped build.',
  phases: [
    { title: 'Implementation', detail: 'coder (sonnet, medium) implements the plan or fixes the last failure' },
    { title: 'Verification', detail: 'verifier runs the mandatory gates; a failure goes back to Implementation' },
    { title: 'Code review', detail: 'code-reviewer (opus, medium); a blocking finding goes back to Implementation' },
    { title: 'Demo', detail: 'demo records the devices and writes the local HTML presentation' },
  ],
}

const GATE_IDS = ['integrity', 'detekt', 'unit', 'coverage', 'e2e-desktop', 'e2e-android', 'e2e-ios']
const UI_MODES = ['added', 'changed', 'none']
const PLATFORMS = ['android', 'ios']
const DEFAULT_LIMITS = { attempts: 5, rounds: 3, stall: 3 }

const IMPLEMENTATION = {
  type: 'object',
  required: ['summary', 'commits', 'blockers'],
  properties: {
    summary: { type: 'string' },
    commits: { type: 'array', items: { type: 'string' } },
    blockers: { type: 'array', items: { type: 'string' } },
  },
}

const VERIFICATION = {
  type: 'object',
  required: ['status'],
  properties: {
    status: { type: 'string', enum: ['done', 'error'] },
    run: { type: 'string' },
    base: { type: 'string' },
    head: { type: 'string' },
    message: { type: 'string' },
    gates: {
      type: 'array',
      items: {
        type: 'object',
        required: ['id', 'status', 'exitCode'],
        properties: {
          id: { type: 'string' },
          status: { type: 'string' },
          exitCode: { type: ['integer', 'null'] },
          durationSec: { type: ['number', 'null'] },
          log: { type: ['string', 'null'] },
          fingerprint: { type: 'string' },
          excerpt: { type: 'string' },
        },
      },
    },
  },
}

const REVIEW = {
  type: 'object',
  required: ['findings'],
  properties: {
    findings: {
      type: 'array',
      items: {
        type: 'object',
        required: ['standpoint', 'severity', 'problem', 'change'],
        properties: {
          standpoint: { type: 'string', enum: ['intent', 'conventions', 'bugs', 'style', 'cheating'] },
          severity: { type: 'string', enum: ['blocking', 'nit'] },
          file: { type: 'string' },
          line: { type: 'integer' },
          problem: { type: 'string' },
          change: { type: 'string' },
        },
      },
    },
  },
}

const DEMO = {
  type: 'object',
  required: ['path', 'media', 'highlights', 'problems'],
  properties: {
    path: { type: ['string', 'null'] },
    media: { type: 'array', items: { type: 'string' } },
    highlights: { type: 'array', items: { type: 'string' } },
    problems: { type: 'array', items: { type: 'string' } },
  },
}

const input = args ?? {}
const problems = []
for (const key of ['slug', 'branch', 'featureDir', 'baseSha', 'plan']) {
  if (typeof input[key] !== 'string' || !input[key].trim()) problems.push(`${key} is missing`)
}
if (typeof input.slug === 'string' && !/^[a-z0-9][a-z0-9-]*$/.test(input.slug)) problems.push('slug must be lowercase words joined by hyphens')
if (typeof input.baseSha === 'string' && !/^[0-9a-f]{7,40}$/.test(input.baseSha)) problems.push('baseSha must be a commit hash')
const gates = Array.isArray(input.gates) ? GATE_IDS.filter(id => input.gates.includes(id)) : []
const unknownGates = (Array.isArray(input.gates) ? input.gates : []).filter(id => !GATE_IDS.includes(id))
if (unknownGates.length) problems.push(`unknown gates: ${unknownGates.join(', ')}`)
if (!gates.includes('integrity')) problems.push('gates must include integrity')
if (!UI_MODES.includes(input.ui)) problems.push(`ui must be one of ${UI_MODES.join(', ')}`)
const platforms = Array.isArray(input.demo?.platforms) ? input.demo.platforms : []
const unknownPlatforms = platforms.filter(platform => !PLATFORMS.includes(platform))
if (unknownPlatforms.length) problems.push(`unknown demo platforms: ${unknownPlatforms.join(', ')}`)
const allow = Array.isArray(input.allowProtected) ? input.allowProtected : []
const unsafeAllow = allow.filter(pattern => typeof pattern !== 'string' || !/^[A-Za-z0-9._*\/-]+$/.test(pattern))
if (unsafeAllow.length) problems.push(`allowProtected entries may hold only letters, digits and . _ * / -: ${unsafeAllow.join(', ')}`)
const limits = { ...DEFAULT_LIMITS, ...(input.limits ?? {}) }
for (const key of Object.keys(DEFAULT_LIMITS)) {
  if (!Number.isInteger(limits[key]) || limits[key] < 1) problems.push(`limits.${key} must be a positive integer`)
}
if (problems.length) return { status: 'error', problems }

const artifacts = input.artifacts ?? {}
const context = [
  `Feature: ${input.slug}, branch ${input.branch}. Base commit: ${input.baseSha}.`,
  `Business spec: ${artifacts.businessSpec ?? `${input.featureDir}/business-spec.md`}`,
  `Technical design: ${artifacts.techDesign ?? 'none'}`,
  `Decision records: ${(artifacts.decisions ?? []).join(', ') || 'none'}`,
  `Design artifacts: ${(artifacts.design ?? []).join(', ') || 'none'}`,
  `Mandatory gates: ${gates.join(', ')}`,
  '',
  '## Plan',
  '',
  input.plan,
].join('\n')

const history = []
let nits = []

function stop(reason, detail) {
  log(`stopped: ${reason}`)
  return { status: 'failed', reason, detail, slug: input.slug, branch: input.branch, history, nits }
}

function startCommand(run) {
  const allowFlags = allow.map(pattern => ` --allow '${pattern}'`).join('')
  return `git show ${input.baseSha}:scripts/feature-factory/gates.py | python3 - start --base ${input.baseSha} --slug ${input.slug} --run ${run}${allowFlags} ${gates.join(' ')}`
}

function evaluate(verification, run) {
  if (!verification) return { kind: 'environment', message: `the verifier of ${run} returned no result` }
  if (verification.status !== 'done') return { kind: 'environment', message: verification.message ?? 'the gate runner reported an error' }
  if (verification.run !== run || !String(verification.base ?? '').startsWith(input.baseSha)) {
    return { kind: 'environment', message: `the verifier returned a result for another run: ${verification.run} on ${verification.base}` }
  }
  const byId = Object.fromEntries((verification.gates ?? []).map(gate => [gate.id, gate]))
  const environment = gates.filter(id => byId[id]?.status === 'env-error')
  if (environment.length) return { kind: 'environment', message: environment.map(id => `${id}: ${byId[id].excerpt}`).join('\n') }
  const failing = gates.filter(id => byId[id]?.status !== 'pass' || byId[id]?.exitCode !== 0)
  return { kind: failing.length ? 'fail' : 'pass', failing, byId }
}

function gateFeedback(verdict) {
  return verdict.failing.map(id => {
    const gate = verdict.byId[id]
    if (!gate) return `### Gate ${id}: no result\nThe gate runner reported nothing for this gate.`
    const code = gate.exitCode === null || gate.exitCode === undefined ? '' : ` (exit code ${gate.exitCode})`
    return `### Gate ${id}: ${gate.status}${code}\nFull log: ${gate.log ?? 'none'}\n${gate.excerpt ?? ''}`
  }).join('\n\n')
}

function findingsText(findings) {
  return findings.map((finding, index) => {
    const place = finding.file ? `${finding.file}${finding.line ? `:${finding.line}` : ''}: ` : ''
    return `${index + 1}. [${finding.standpoint}] ${place}${finding.problem}\n   Required change: ${finding.change}`
  }).join('\n')
}

function gateSummary(verification) {
  return [`head ${verification.head}`, ...(verification.gates ?? []).map(gate => `- ${gate.id}: ${gate.status}`)].join('\n')
}

let feedback = null
let lastVerification = null
let earlierBlocking = []
let lastKey = null
let repeats = 0
let reviewed = false
let rounds = 0

for (let round = 1; round <= limits.rounds && !reviewed; round++) {
  rounds = round
  let verified = false
  for (let attempt = 1; attempt <= limits.attempts && !verified; attempt++) {
    const run = `r${round}a${attempt}`
    const task = feedback ? `## Feedback to fix\n\n${feedback}` : 'Implement the plan.'
    const work = await agent(`${context}\n\n${task}`, { agentType: 'coder', phase: 'Implementation', label: `implement ${run}`, schema: IMPLEMENTATION })
    if (!work) return stop('agent-died', `the coder of ${run} returned no result`)
    history.push({ step: 'implementation', run, summary: work.summary, commits: work.commits })
    if (work.blockers.length) return stop('blocked', work.blockers)

    const verification = await agent(`Start command:\n${startCommand(run)}`, { agentType: 'verifier', phase: 'Verification', label: `gates ${run}`, schema: VERIFICATION })
    const verdict = evaluate(verification, run)
    history.push({ step: 'verification', run, verdict: verdict.kind, failing: verdict.failing ?? [] })
    if (verdict.kind === 'environment') return stop('environment', verdict.message)
    lastVerification = verification
    if (verdict.kind === 'pass') {
      verified = true
      log(`${run}: all mandatory gates passed`)
      break
    }
    log(`${run}: failing gates ${verdict.failing.join(', ')}`)
    const key = verdict.failing.map(id => `${id}:${verdict.byId[id]?.fingerprint || verdict.byId[id]?.status || 'missing'}`).join('|')
    repeats = key === lastKey ? repeats + 1 : 1
    lastKey = key
    if (repeats >= limits.stall) return stop('stalled', `the same gate failure repeated ${repeats} times:\n${gateFeedback(verdict)}`)
    feedback = gateFeedback(verdict)
  }
  if (!verified) return stop('verification-exhausted', `the gates still fail after ${limits.attempts} attempts in round ${round}:\n${feedback}`)

  const earlier = earlierBlocking.length ? `\n\n## Earlier blocking findings\n\n${findingsText(earlierBlocking)}` : ''
  const reviewTask = `## Gate results of the last verification\n\n${gateSummary(lastVerification)}${earlier}\n\nReview git diff ${input.baseSha}..HEAD.`
  const review = await agent(`${context}\n\n${reviewTask}`, { agentType: 'code-reviewer', phase: 'Code review', label: `review r${round}`, schema: REVIEW })
  if (!review) return stop('agent-died', `the code reviewer of round ${round} returned no result`)
  const blocking = review.findings.filter(finding => finding.severity === 'blocking')
  nits = review.findings.filter(finding => finding.severity === 'nit')
  history.push({ step: 'review', round, blocking: blocking.length, nits: nits.length })
  if (!blocking.length) {
    reviewed = true
    log(`review r${round}: passed`)
    break
  }
  log(`review r${round}: ${blocking.length} blocking findings`)
  earlierBlocking = blocking
  feedback = `The code review failed. Fix these blocking findings:\n\n${findingsText(blocking)}`
}
if (!reviewed) return stop('review-exhausted', findingsText(earlierBlocking))

const demoTask = [
  `Feature: ${input.slug}. Base commit: ${input.baseSha}. ui: ${input.ui}. Platforms: ${platforms.join(', ') || 'none'}.`,
  `Business spec: ${artifacts.businessSpec ?? `${input.featureDir}/business-spec.md`}`,
  `Technical design: ${artifacts.techDesign ?? 'none'}`,
  `Design artifacts: ${(artifacts.design ?? []).join(', ') || 'none'}`,
  '',
  '## Demo spec',
  '',
  input.demo?.spec ?? 'No demo spec: show the highlights only.',
  '',
  '## Gate results',
  '',
  gateSummary(lastVerification),
  '',
  '## Review nits',
  '',
  nits.length ? findingsText(nits) : 'none',
  '',
  '## Plan',
  '',
  input.plan,
].join('\n')
const demo = await agent(demoTask, { agentType: 'demo', phase: 'Demo', label: 'demo', schema: DEMO })

return {
  status: 'passed',
  slug: input.slug,
  branch: input.branch,
  head: lastVerification.head,
  rounds,
  gates: lastVerification.gates.map(gate => ({ id: gate.id, status: gate.status })),
  nits,
  history,
  demo: demo ?? { path: null, media: [], highlights: [], problems: ['the demo agent returned no result'] },
}
