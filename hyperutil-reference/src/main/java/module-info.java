module xyz.ielis.hyperutil.reference {
    exports xyz.ielis.hyperutil.reference.fasta;

    requires com.google.common;
    requires htsjdk;
    requires jannovar.core;

    opens xyz.ielis.hyperutil.reference.fasta;
}