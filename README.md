# Schmoopie

## How to build

1. Run `mvn clean package`.
2. The file `schmoopie-<Version>.jar` will be in the `target` directory.
3. Put the resulting binary into `~/sw/schmoopie`.
4. Call the application using a script like this in `~/dev/misc/intranet/protocols`:

```shell
#!/bin/bash

filename="reviews/r-$(date +%Y-%m-%d).org"
echo "$filename"

{
    cat template-schmoopie-header.org
    cat ../m2026/ctl.fodp | java -jar ~/sw/schmoopie/schmoopie-1.0.jar
} > "$filename"
```

The above script assumes that the presentation `ctl.fodp` (similar to `src/test/resources/2026_05_24_map.fodp`) is located in the directory `~/dev/misc/intranet/m2026`.

The resulting review file will be located in `~/dev/misc/intranet/protocols/reviews`.