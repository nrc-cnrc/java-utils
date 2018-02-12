use strict;

my $file = $ARGV[0];
my $start = $ARGV[1];
my $end = $ARGV[2];
if (!defined($end)) {$end = $start;}

if (!defined($start)) {usage();}

print "Printing lines $start to $end of file $file\n";

open FILE, "<$file" | die "Could not open file $file for input";
my $lineNum = 1;

while( my $line = <FILE>)  {
  if ($lineNum >=  $start && $lineNum <= $end) {  
    print "$lineNum: $line\n";
  }
  $lineNum++;
}


sub usage() {
   die "\nUsage: printLines file first last?\n\n";
}