module xyz.ielis.hyperutil.reference {
    exports xyz.ielis.hyperutil.reference.fasta;

    requires com.google.common;
    requires htsjdk;
    requires jannovar.core;
    requires slf4j.api;

    opens xyz.ielis.hyperutil.reference.fasta;
}