# Demo screen

`demo-screen.json` is a showcase screen built only from the five SSUI primitives
(`COLUMN`, `ROW`, `TEXT`, `BUTTON`, `IMAGE`): a dark hero header, an edge-to-edge photo,
three coloured stat tiles, three destination cards with images, a detail card with two action
buttons (one opens a URL, one shows a toast) and a footer. Tapping any photo shows a toast too.

![demo screen](demo-screen.png)

## Show it in the app

The app always loads the screen named `home`, so upload the demo under that name and tap the
refresh icon in the app:

```bash
./demo/apply.sh                 # demo-screen.json -> home
./demo/restore-home.sh          # put the original seed example back
```

Suggested demo flow:

1. Open the app with the default `home` screen.
2. Run `./demo/apply.sh`, tap refresh: the whole screen changes with no app rebuild.
3. Edit a value live, e.g. the greeting or a tile colour, and refresh again:

   ```bash
   BASE=http://localhost:8080/api/v1
   curl -s $BASE/screens/home \
     | jq '(.. | objects | select(.textContent? == "Good morning, Mikhail") | .textContent) = "Hello, audience!"' \
     | curl -s -X PUT -H 'Content-Type: application/json' -d @- $BASE/screens/home
   ```

4. Tap "Book trip" (toast from the server), "Details" (opens Wikipedia), or a destination photo.
5. `./demo/restore-home.sh` to go back.

## Notes

- Images come from picsum.photos, so the emulator needs internet access. The first load takes a
  second or two; subsequent loads are cached by Coil.
- Tile and card widths are fixed at 118 dp (three per row plus gaps fit a 411 dp+ wide phone,
  which covers Pixel-class devices and the default emulators). Rows have no weight support yet.
- The spec's `_id` vs `id`: this file uses `id`, which is what the API accepts. The backend keeps
  the existing Mongo `_id` of `home` when you replace it.
