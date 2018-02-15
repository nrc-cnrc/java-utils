use strict;

my $file = $ARGV[0];
my $regex = $ARGV[1];
my $maxContextSize = $ARGV[2];

if (!defined($file) || !defined($regex)) {usage();}
if (!defined($maxContextSize)) {$maxContextSize = 10;}

print "Printing lines matching '$regex' in file $file\n";

open FILE, "<$file" || die "Could not open file $file for input";
my $lineNum = 1;
my $contextFirstLineNum = 1;
my $contextLines = [];

while( my $line = <FILE>)  {
    $contextFirstLineNum = processLine($line, $lineNum, $regex, $maxContextSize, $contextFirstLineNum,  $contextLines);
    $lineNum++;
}


sub processLine {
    my ($line, $lineNum, $regex, $maxContextSize, $contextFirstLineNum, $contextLines) = @_;
#    print STDERR "-- \$contextFirstLineNum=$contextFirstLineNum, \$maxContextSize=$maxContextSize, \@\$contextLines=".@$contextLines."\n";
    push(@$contextLines, $line);
    my $context = join("\n", @$contextLines);
    if ($context =~ /$regex/) {
	printRegionWithLines($contextLines, $contextFirstLineNum);
    }
    if (@$contextLines > $maxContextSize) {
	shift(@$contextLines);
	$contextFirstLineNum++;
    }

    return ($contextFirstLineNum);
    
}

sub printRegionWithLines {
    my ($contextLines, $contextFirstLineNum) =  @_;

    #print STDERR "-- \$contextFirstLineNum=$contextFirstLineNum, \@\$contextLines=".@$contextLines;
    print "\n>>> Records START <<<\n";
    $lineNum = $contextFirstLineNum;
    foreach my $aLine (@$contextLines) {
	print("$lineNum: $aLine");
	$lineNum++;
    }
    print();
    print ">>> Records END <<<\n";
    
}

sub usage() {
    my $mess = 
	"\nUsage: find_csv_records csvFile regexp maxContextSize\n".
	"\n".
	"Find records in a CSV file that match a particular regexp.\n".
	"The records may span more than one line in the file.\n".
	"\n".
	"ARGUMENTS:\n".
	"\n".
	"  csvFile: path to file\n".
	"\n".
	"  regexp: regexp to match (it may cover more than just the current line\n".
	"\n".
	"  maxContextSize: Number of lines BEFORE current line to take into account\n".
	"    when matching regexp.\n".

	"";
    die($mess);
}
