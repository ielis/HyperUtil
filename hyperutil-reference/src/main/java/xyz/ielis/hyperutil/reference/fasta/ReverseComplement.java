package xyz.ielis.hyperutil.reference.fasta;

import java.util.HashMap;
import java.util.Map;

class ReverseComplement {

    private static final Map<Character, Character> IUPAC = makeIupacMap();

    /**
     * Get reverse complement of a nucleotide sequence <code>seq</code>. The sequence is expected to consist of IUPAC
     * nucleotide symbols. Both upper/lower cases are recognized.
     * <p>
     * If a non-IUPAC symbol is found in <code>seq</code>, an <code>N</code> is put into the position.
     *
     * @param seq nucleotide sequence to reverse complement
     * @return reverse complemented sequence
     */
    static String reverseComplement(String seq) {
        char[] oldSeq = seq.toCharArray();
        char[] newSeq = new char[oldSeq.length];
        for (int i = 0; i < oldSeq.length; i++) {
            newSeq[oldSeq.length - i - 1] = IUPAC.getOrDefault(oldSeq[i], 'N');
        }
        return new String(newSeq);
    }

    private static Map<Character, Character> makeIupacMap() {
        Map<Character, Character> temporary = new HashMap<>();
        temporary.putAll(
                Map.of(
                        // STANDARD
                        'A', 'T',
                        'a', 't',
                        'C', 'G',
                        'c', 'g',
                        'G', 'C',
                        'g', 'c',
                        'T', 'A',
                        't', 'a',
                        'U', 'A',
                        'u', 'a'));
        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 1st part
                        'W', 'W', // weak - A,T
                        'w', 'w',
                        'S', 'S', // strong - C,G
                        's', 's',
                        'M', 'K', // amino - A,C
                        'm', 'k',
                        'K', 'M', // keto - G,T
                        'k', 'm'));
        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 2nd part
                        'R', 'Y', // purine - A,G
                        'r', 'y', // purine - A,G
                        'Y', 'R', // pyrimidine - C,T
                        'y', 'r')); // pyrimidine - C,T

        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 3rd part
                        'B', 'V', // not A
                        'b', 'v', // not A
                        'D', 'H', // not C
                        'd', 'h', // not C
                        'H', 'D', // not G
                        'h', 'd', // not G
                        'V', 'B', // not T
                        'v', 'b', // not T
                        'N', 'N', // any one base
                        'n', 'n' // any one base
                )
        );
        return temporary;
    }
}
