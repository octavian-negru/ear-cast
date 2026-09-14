# SpeexDSP resampler provenance

The added resampler files come unchanged from official tag `SpeexDSP-1.2.1`:

- `include/speex/speex_resampler.h`
- `libspeexdsp/resample.c`
- `libspeexdsp/resample_sse.h`
- `libspeexdsp/resample_neon.h`

Source: <https://github.com/xiph/speexdsp/tree/SpeexDSP-1.2.1>.
`RESAMPLER_SHA256SUMS` records these files. The existing `COPYING`, authors and
source notices apply; retain them in distributions.

RNNoise uses this library only for stateful float sample-rate conversion at
quality 10. The older Speex preprocessor remains available as a comparison
baseline but is not cascaded with neural suppression in Hearing Assist.
