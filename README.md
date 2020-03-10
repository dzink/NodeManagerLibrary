# Node Manager Library


## Examples

Respond to note on and note off with the default server and default synthdef:

```supercollider
  ~n = NmlKeys();
	~n.midiConnectNoteOn();
	~n.midiConnectNoteOff();
```

Create a mono version of the same setup:

```supercollider
  ~n = NmlMono();
	~n.midiConnectNoteOn();
	~n.midiConnectNoteOff();
```

Respond to note on and note off, plus polytouch, with a selected group and custom synthdef:

```supercollider
  ~group = Group();
  ~n = NmlKeys(~group, \customKey);
	~n.midiConnectNoteOn();
	~n.midiConnectNoteOff();
	~n.midiConnectPolytouch();
```