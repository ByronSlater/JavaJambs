// @jimp/core references the Node `Buffer` global at module top-level (not
// just inside function bodies), so this needs to be imported before any
// @jimp/* module in order to be defined in time.
import { Buffer } from 'buffer'

globalThis.Buffer ??= Buffer
