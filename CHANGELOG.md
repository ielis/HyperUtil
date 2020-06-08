# Changelog

## v0.1.3
- remove requirement for nucleotide sequence to match `[ACGTNacgtn]+`
- improve reverse complementing implementation, now works with all *IUPAC* bases, while non-*IUPAC* bases (e.g. `X`) are self-complementary 

## v0.1.1
- implement `GenomeSequenceAccessorBuilder`
- deprecate `GenomeSequenceAccessors`

## v0.1.0
- implement `SequenceInterval` and appropriate method in `GenomeSequenceAccessor` (breaking change)
- use HTSJDK `v2.19.0` in order to match the rest of the ecosystem

## v0.0.3
- implement `SingleChromosomeGenomeSequenceAccessor`
