// Sample idle CSS animations without changing easing or wall-clock duration.
// Active and one-shot animations remain browser-driven. SVG scripts are sanitized.
(function () {
  var idle = false;
  var timer = null;
  var tracked = new Map();
  function restore() {
    tracked.forEach(function (entry, animation) {
      var target = animation.effect && animation.effect.target;
      if ((!target || target.isConnected !== false) && animation.playState !== 'finished') animation.play();
    });
    tracked.clear();
  }
  function sample() {
    if (!idle || typeof document.getAnimations !== 'function') return;
    var now = performance.now();
    var live = new Set(document.getAnimations());
    tracked.forEach(function (entry, animation) {
      if (!live.has(animation)) tracked.delete(animation);
    });
    live.forEach(function (animation) {
      if (!tracked.has(animation) && animation.playState === 'running') {
        tracked.set(animation, { time: Number(animation.currentTime) || 0, start: now });
        animation.pause();
      }
      var entry = tracked.get(animation);
      if (entry) animation.currentTime = entry.time + now - entry.start;
    });
    // No timer for genuinely static artwork.
    if (tracked.size) timer = setTimeout(sample, 50);
  }
  window.refreshPetPower = function () {
    clearTimeout(timer);
    timer = null;
    restore();
    sample();
  };
  window.setPetIdle = function (value) {
    idle = !!value;
    window.refreshPetPower();
  };
})();
