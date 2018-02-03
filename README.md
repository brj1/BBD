# BBD: BEAST 2 package for blind-dating

Bradley R. Jones

To install on MAC:

```
cd dist
unzip BBD.v1.0.3.zip
mkdir ~/Library/Application\ Support/BEAST/2.4/BBD
cp -r * ~/Library/Application\ Support/BEAST/2.4/BBD/
```

To add prior in BEATUi 2:

1. Add aligment and tip dates
2. Go to Priors tab and click the "+ Add Prior Button"
3. Select "BBD Prior" and click OK
4. Choose taxa to date and give taxon saet a label
5. After clicking OK BEAUTi will give a warning message: "Could not add entry for distr". This can be ignored.
6. Select your prior distribution and you're off.
