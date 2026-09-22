export function init(root) {
  if (!root) return;

  root.querySelectorAll('[data-carousel]').forEach((viewport) => {
    const type = viewport.dataset.carousel;
    const track = viewport.querySelector('.carousel__track');
    const input = root.querySelector(`input[data-type="${type}"]`);
    if (!track || !input) return;

    const getItems = () => track.querySelectorAll('.carousel__item[data-clothes-id]');

    const closestItemIndex = (items) => {
      const trackRect = track.getBoundingClientRect();
      const trackCenter = trackRect.left + trackRect.width / 2;

      let closestIndex = 0;
      let closestDistance = Infinity;
      items.forEach((item, index) => {
        const itemRect = item.getBoundingClientRect();
        const itemCenter = itemRect.left + itemRect.width / 2;
        const distance = Math.abs(itemCenter - trackCenter);
        if (distance < closestDistance) {
          closestDistance = distance;
          closestIndex = index;
        }
      });
      return closestIndex;
    };

    const updateCentered = () => {
      const items = getItems();
      if (!items.length) {
        input.disabled = true;
        input.value = '';
        return;
      }

      input.disabled = false;
      input.value = items[closestItemIndex(items)].dataset.clothesId;
    };

    const scrollToIndex = (index, behavior = 'smooth') => {
      const items = getItems();
      if (!items.length) return;

      const clampedIndex = Math.max(0, Math.min(index, items.length - 1));
      const target = items[clampedIndex];

      const trackRect = track.getBoundingClientRect();
      const targetRect = target.getBoundingClientRect();
      const delta = (targetRect.left + targetRect.width / 2) - (trackRect.left + trackRect.width / 2);

      track.scrollTo({ left: track.scrollLeft + delta, behavior });
    };

    let scrollTimer;
    track.addEventListener(
      'scroll',
      () => {
        clearTimeout(scrollTimer);
        scrollTimer = setTimeout(updateCentered, 100);
      },
      { passive: true }
    );

    // Best-effort immediately, then again once images (some several MB) have
    // finished loading and settled the track's true layout - otherwise the
    // first item can land off-center or scrolled out of view entirely.
    const alignToFirstItem = () => {
      scrollToIndex(0, 'auto');
      updateCentered();
    };
    alignToFirstItem();
    if (document.readyState === 'complete') {
      alignToFirstItem();
    } else {
      window.addEventListener('load', alignToFirstItem);
    }
  });
}
