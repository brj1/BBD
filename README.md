# BBD: BEAST 2 package for extended tip date sampling
Bradley R. Jones

To install on MAC:

```
cd dist
unzip BBD.v1.0.4.zip
mkdir ~/Library/Application\ Support/BEAST/2.6/BBD
cp -r * ~/Library/Application\ Support/BEAST/2.6/BBD/
```

To add prior in BEAUTi 2:

1. Add aligment and tip dates
2. Go to Priors tab and click the "+ Add Prior Button"
3. Select "BBD Prior" and click OK
4. Choose taxa to date, give taxon set a label and click OK
6. Select your prior distribution and starting date probability and you're off.
