// Spike: can Jimp's pixel-manipulation core run entirely in the browser and
// power an interactive "magic wand" background remover?
//
// Decode/encode (PNG/JPEG/etc.) stays with the browser's own <canvas>/<img> -
// Jimp's codec plugins (@jimp/js-png -> pngjs, etc.) pull in Node builtins
// (zlib/stream/util) that don't exist in a browser. Jimp is used only for its
// format-agnostic pixel engine (fromBitmap/getPixelColor/setPixelColor/scan),
// built with zero formats/plugins so none of that codec code is even bundled.
// Must run before @jimp/core is evaluated: it references the Node `Buffer`
// global at module top-level (default option objects), so the polyfill has
// to land first. Static imports in this file evaluate top-to-bottom before
// this module's own body runs, so importing it first is enough.
import './buffer-polyfill.mjs'
import { createJimp } from '@jimp/core'
import { intToRGBA } from '@jimp/utils'

const Jimp = createJimp({ formats: [], plugins: [] })

const MAX_DIMENSION = 1000

export function init(root) {
  const fileInput = root.querySelector('[data-jimp-file]')
  const canvas = root.querySelector('[data-jimp-canvas]')
  const toleranceInput = root.querySelector('[data-jimp-tolerance]')
  const toleranceValue = root.querySelector('[data-jimp-tolerance-value]')
  const wandControls = root.querySelector('[data-jimp-wand-controls]')
  const brushControls = root.querySelector('[data-jimp-brush-controls]')
  const brushSizeInput = root.querySelector('[data-jimp-brush-size]')
  const brushSizeValue = root.querySelector('[data-jimp-brush-size-value]')
  const brushCursor = root.querySelector('[data-jimp-brush-cursor]')
  const toolButtons = root.querySelectorAll('[data-jimp-tool]')
  const resetButton = root.querySelector('[data-jimp-reset]')
  const undoButton = root.querySelector('[data-jimp-undo]')
  const downloadButton = root.querySelector('[data-jimp-download]')
  const saveButton = root.querySelector('[data-jimp-save]')
  const status = root.querySelector('[data-jimp-status]')
  const savedResult = root.querySelector('[data-jimp-saved-result]')
  const ctx = canvas.getContext('2d', { willReadFrequently: true })

  let originalImageData = null
  let toolMode = 'wand'
  let isBrushing = false
  let lastBrushPoint = null
  const undoStack = []

  function canvasPointFromEvent(event) {
    const rect = canvas.getBoundingClientRect()
    return {
      x: ((event.clientX - rect.left) / rect.width) * canvas.width,
      y: ((event.clientY - rect.top) / rect.height) * canvas.height,
      scale: rect.width / canvas.width,
    }
  }

  function setToolMode(mode) {
    toolMode = mode
    for (const button of toolButtons) {
      const isActive = button.dataset.jimpTool === mode
      button.classList.toggle('btn-active', isActive)
      button.setAttribute('aria-pressed', String(isActive))
    }
    wandControls.classList.toggle('hidden', mode !== 'wand')
    brushControls.classList.toggle('hidden', mode !== 'brush')
    canvas.style.cursor = mode === 'brush' ? 'none' : 'crosshair'
    brushCursor.classList.add('hidden')
  }

  for (const button of toolButtons) {
    button.addEventListener('click', () => setToolMode(button.dataset.jimpTool))
  }
  setToolMode(toolMode)

  function setStatus(message, isError = false) {
    status.textContent = message
    status.classList.toggle('text-error', isError)
    status.classList.toggle('text-base-content/60', !isError)
  }

  function setControlsEnabled(enabled) {
    for (const button of [resetButton, undoButton, downloadButton, saveButton]) {
      button.disabled = !enabled
    }
  }

  fileInput.addEventListener('change', async () => {
    const file = fileInput.files[0]
    if (!file) return

    try {
      await loadFileToCanvas(file)
      setControlsEnabled(true)
      undoStack.length = 0
      undoButton.disabled = true
      setStatus(`Loaded ${file.name} (${canvas.width}x${canvas.height}). Magic-wand click a region, or switch to Brush to erase by hand.`)
    } catch (error) {
      console.error(error)
      setStatus(`Could not load that image: ${error.message}`, true)
    }
  })

  async function loadFileToCanvas(file) {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height))
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)

    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()

    originalImageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
  }

  canvas.addEventListener('click', (event) => {
    if (toolMode !== 'wand' || !originalImageData) return

    const { x: rawX, y: rawY } = canvasPointFromEvent(event)
    const x = Math.floor(rawX)
    const y = Math.floor(rawY)
    if (x < 0 || y < 0 || x >= canvas.width || y >= canvas.height) return

    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    undoStack.push(imageData)
    undoButton.disabled = false

    const tolerance = Number(toleranceInput.value)
    const result = magicWandErase(imageData, x, y, tolerance)
    ctx.putImageData(result, 0, 0)
    setStatus(`Erased the region matching the pixel at (${x}, ${y}) with tolerance ${tolerance}.`)
  })

  toleranceInput.addEventListener('input', () => {
    toleranceValue.textContent = toleranceInput.value
  })

  brushSizeInput.addEventListener('input', () => {
    brushSizeValue.textContent = brushSizeInput.value
    if (!brushCursor.classList.contains('hidden')) {
      sizeBrushCursor()
    }
  })

  function sizeBrushCursor() {
    const rect = canvas.getBoundingClientRect()
    const diameter = Number(brushSizeInput.value) * (rect.width / canvas.width)
    brushCursor.style.width = `${diameter}px`
    brushCursor.style.height = `${diameter}px`
  }

  canvas.addEventListener('pointermove', (event) => {
    if (toolMode === 'brush' && originalImageData) {
      sizeBrushCursor()
      brushCursor.classList.remove('hidden')
      brushCursor.style.left = `${event.offsetX}px`
      brushCursor.style.top = `${event.offsetY}px`
    }

    if (isBrushing) {
      brushEraseAt(event)
    }
  })

  canvas.addEventListener('pointerleave', () => {
    brushCursor.classList.add('hidden')
  })

  canvas.addEventListener('pointerdown', (event) => {
    if (toolMode !== 'brush' || !originalImageData) return

    isBrushing = true
    lastBrushPoint = null
    canvas.setPointerCapture(event.pointerId)

    undoStack.push(ctx.getImageData(0, 0, canvas.width, canvas.height))
    undoButton.disabled = false

    ctx.save()
    ctx.globalCompositeOperation = 'destination-out'
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'

    brushEraseAt(event)
  })

  function endBrushStroke() {
    if (!isBrushing) return
    isBrushing = false
    lastBrushPoint = null
    ctx.restore()
    setStatus('Erased with the brush.')
  }

  canvas.addEventListener('pointerup', endBrushStroke)
  canvas.addEventListener('pointercancel', endBrushStroke)

  function brushEraseAt(event) {
    const { x, y } = canvasPointFromEvent(event)
    const radius = Number(brushSizeInput.value) / 2

    ctx.lineWidth = radius * 2
    if (lastBrushPoint) {
      ctx.beginPath()
      ctx.moveTo(lastBrushPoint.x, lastBrushPoint.y)
      ctx.lineTo(x, y)
      ctx.stroke()
    }

    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.fill()

    lastBrushPoint = { x, y }
  }

  resetButton.addEventListener('click', () => {
    if (!originalImageData) return
    undoStack.length = 0
    undoButton.disabled = true
    ctx.putImageData(originalImageData, 0, 0)
    setStatus('Reset to the original upload.')
  })

  undoButton.addEventListener('click', () => {
    const previous = undoStack.pop()
    if (!previous) return
    ctx.putImageData(previous, 0, 0)
    undoButton.disabled = undoStack.length === 0
    setStatus('Undid the last edit.')
  })

  downloadButton.addEventListener('click', () => {
    canvas.toBlob((blob) => {
      if (!blob) return
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'jimp-edit.png'
      link.click()
      URL.revokeObjectURL(url)
    }, 'image/png')
  })

  saveButton.addEventListener('click', () => {
    canvas.toBlob(async (blob) => {
      if (!blob) return

      try {
        setStatus('Uploading...')
        const formData = new FormData()
        formData.append('image', blob, 'jimp-edit.png')
        formData.append('path', root.dataset.uploadPath || 'jimp-mockup')

        const response = await fetch('/img/upload', {
          method: 'POST',
          headers: { [root.dataset.csrfHeader]: root.dataset.csrfToken },
          body: formData,
        })

        if (!response.ok) {
          throw new Error(`Upload failed with status ${response.status}`)
        }

        const html = await response.text()
        savedResult.innerHTML = html

        const returnTo = root.dataset.returnTo
        if (returnTo && /^\/(?!\/)/.test(returnTo)) {
          const url = new DOMParser().parseFromString(html, 'text/html').querySelector('img')?.getAttribute('src')
          if (url) {
            setStatus('Saved. Returning...')
            window.location.href = `${returnTo}?imageUrl=${encodeURIComponent(url)}`
            return
          }
        }

        setStatus('Saved.')
      } catch (error) {
        console.error(error)
        setStatus(`Could not save: ${error.message}`, true)
      }
    }, 'image/png')
  })
}

/**
 * Flood-fills from (startX, startY) over pixels within `tolerance` (0-100)
 * of the starting color, erasing them to transparent. Uses Jimp's
 * format-agnostic pixel engine (fromBitmap/getPixelColor/setPixelColor) as
 * the actual image-editing primitive.
 */
function magicWandErase(imageData, startX, startY, tolerance) {
  const image = Jimp.fromBitmap({
    width: imageData.width,
    height: imageData.height,
    data: imageData.data,
  })

  const { width, height } = image.bitmap
  const maxDistance = Math.sqrt(3 * 255 * 255)
  const threshold = (tolerance / 100) * maxDistance
  const targetColor = intToRGBA(image.getPixelColor(startX, startY))

  const visited = new Uint8Array(width * height)
  const stack = [[startX, startY]]

  while (stack.length > 0) {
    const [x, y] = stack.pop()
    if (x < 0 || y < 0 || x >= width || y >= height) continue

    const index = y * width + x
    if (visited[index]) continue
    visited[index] = 1

    const color = intToRGBA(image.getPixelColor(x, y))
    const distance = Math.sqrt(
      (color.r - targetColor.r) ** 2 +
        (color.g - targetColor.g) ** 2 +
        (color.b - targetColor.b) ** 2,
    )
    if (distance > threshold) continue

    image.setPixelColor(0x00000000, x, y)

    stack.push([x + 1, y], [x - 1, y], [x, y + 1], [x, y - 1])
  }

  return new ImageData(new Uint8ClampedArray(image.bitmap.data), width, height)
}
