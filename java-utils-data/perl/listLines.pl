use strict;

my ($verbose, $args) = getOptVerbose(\@ARGV);

my $file = $args->[0];
my $start = eval($args->[1]);
my $end = eval($args->[2]);
if (!defined($end)) {$end = $start;}

if (!defined($start)) {usage("Missing first argument.");}

if (defined($verbose)) {
  print "\n\n=== Printing lines $start to $end of file $file\n";
}

open FILE, "<$file" || die "Could not open file $file for input";
my $lineNum = 1;

while( my $line = <FILE>)  {
  if ($lineNum >=  $start && $lineNum <= $end) {  
    my $toPrint = $line;
    if (defined($verbose)) {
      $toPrint = "$lineNum: $toPrint\n";
    }
    print $toPrint;
  }

  last if ($lineNum > $end);
  $lineNum++;
}

close FILE;


sub getOptVerbose {
  my ($args) = @_;
  my $argsNoOption = [];
  my $verbose = undef; 
  foreach my $anArg (@$args) {
    if ($anArg eq "-verbose") {
      $verbose = 1;
    } else {
      push(@$argsNoOption, $anArg);
    }
  }
  return ($verbose, $argsNoOption);
}


sub usage {
   my ($err) = @_;
   my $message =  
   	"\n".
   	"Usage: printLines OPTIONS file first last?\n\n".
   	"  Print a list lines from a file.\n".
   	"\n".
   	"OPTIONS:\n".
   	"\n".
   	"  -verbose: If present, then print line numbers and header."
   	;
   	
   if (defined($err)) {
     $message = "** ERROR: $err\n\n$message";
   }	
   die $message;
}