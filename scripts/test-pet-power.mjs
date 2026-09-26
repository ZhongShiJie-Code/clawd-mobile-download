import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../android/app/src/main/assets/html/pet_power.js', import.meta.url), 'utf8');
let now = 0;
let sequence = 0;
let animations = [];
const timers = new Map();
const window = {};
vm.runInNewContext(source, {
  window,
  document: { getAnimations: () => animations },
  performance: { now: () => now },
  setTimeout: (fn, delay) => { const id = ++sequence; timers.set(id, { fn, at: now + delay }); return id; },
  clearTimeout: id => timers.delete(id),
});
const makeAnimation = () => ({
  currentTime: 100,
  playState: 'running',
  effect: { target: { isConnected: true } },
  plays: 0,
  pause() { this.playState = 'paused'; },
  play() { this.playState = 'running'; this.plays++; },
});
const tick = () => {
  now += 50;
  for (const [id, timer] of [...timers]) {
    if (timer.at <= now) { timers.delete(id); timer.fn(); }
  }
};

window.setPetIdle(true);
assert.equal(timers.size, 0, 'static art must not schedule rendering');
const idle = makeAnimation();
animations = [idle];
window.refreshPetPower();
assert.equal(idle.playState, 'paused');
assert.equal(timers.size, 1);
tick();
assert.equal(idle.currentTime, 150, 'idle playback keeps wall-clock duration');
tick();
assert.equal(idle.currentTime, 200);
window.setPetIdle(false);
assert.equal(idle.playState, 'running', 'working/completion resumes immediately');
assert.equal(timers.size, 0, 'active animation uses browser, not a polling timer');
window.setPetIdle(true);
idle.effect.target.isConnected = false;
animations = [];
const playsBefore = idle.plays;
window.refreshPetPower();
assert.equal(idle.plays, playsBefore, 'removed SVG must never be reactivated');
assert.equal(timers.size, 0);
const replacement = makeAnimation();
animations = [replacement];
window.refreshPetPower();
assert.equal(replacement.playState, 'paused', 'new idle artwork inherits policy');
window.setPetIdle(false);
assert.equal(replacement.playState, 'running');
assert.equal(timers.size, 0);
console.log('Pet power policy: 11 assertions passed');
