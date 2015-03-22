## Matcher: Part 1 Setting up

## Introduction
This document describes how the person running the Matcher service sets it up.

## Prerequisites

The Redeemers have performed the steps described in [Creating Bitcoin wallets](Redeemer-1-Creating-Bitcoin-wallets.md)

This ensures that the Redeemers have created export files containing their Bitcoin addresses and that
these export files have been copied to the Matcher machine

In this document it will be assumed that there are two separate users that have prepared
one export file each and copied them to the directory on the Matcher machine as follows:

    matcher
      import-from-redeemer
        redeemer-1-1.txt
        redeemer-2-1.txt

The steps to set up the Matcher service are:

1) Create a directory in which to store the PGP keypairs.
   (The keypairs are kept separate from any other PGP keypairs on the Matcher machine).
2) Create the Matcher PGP keypair that is used by the Payers to encrypt traffic sent to the Matcher.
   Export this PGP keypair so that it can be copied to the Payers' machines.
3) Tidy up
4) Start the Matcher daemon that accepts requests from the Payers.


## Step 1 Create a GPG directory

Create a directory where the Matcher is located as follows:

    matcher
       gpg                    < GPG details for Matcher
       import-from-redeemer   < Directory into which Redeemers' Bitcoin addresses have been copied

## Step 2 Create the PGP keypair that is used by the Payer

Open a shell and `cd` into the `gpg` directory created above.

The keyring for the Redeemer was constructed using GPG.
This is the log of how it was constructed (on a Mac):
The password used to protect the keyrings was 'password' (obviously use a better password for your real keyrings).

## Step 3 Start the gpg-agent

The GPG agent is required for processing GPG commands:

    > gpg-agent --homedir "$(pwd)" --daemon

## Step 4 Generate a new key pair

Enter following command:

    > gpg --homedir "$(pwd)" --gen-key

You will see this output

    gpg (GnuPG/MacGPG2) 2.0.22; Copyright (C) 2013 Free Software Foundation, Inc.
    This is free software: you are free to change and redistribute it.
    There is NO WARRANTY, to the extent permitted by law.

    Please select what kind of key you want:
       (1) RSA and RSA (default)
       (2) DSA and Elgamal
       (3) DSA (sign only)
       (4) RSA (sign only)
    Your selection? 1

Selecting "1" for RSA...

    RSA keys may be between 1024 and 8192 bits long.
    What keysize do you want? (2048)
    Requested keysize is 2048 bits
    Please specify how long the key should be valid.
             0 = key does not expire
          <n>  = key expires in n days
          <n>w = key expires in n weeks
          <n>m = key expires in n months
          <n>y = key expires in n years
    Key is valid for? (0) 0
    Key does not expire at all
    Is this correct? (y/N) y

Selecting "y" for correct...
                        
    GnuPG needs to construct a user ID to identify your key.

    Real name: matcher
    Email address: matcher@example.org
    Comment:
    You selected this USER-ID:
        "matcher <matcher@example.org>"
    Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? o

Selecting "o" for Okay...

    You need a Passphrase to protect your secret key.

    We need to generate a lot of random bytes. It is a good idea to perform
    some other action (type on the keyboard, move the mouse, utilize the
    disks) during the prime generation; this gives the random number
    generator a better chance to gain enough entropy.

Start wiggling the mouse and hammering the keyboard to build entropy...

    gpg: /brit-example/matcher/gpg/trustdb.gpg: trustdb created
    gpg: key 58614CEE marked as ultimately trusted
    public and secret key created and signed.

    gpg: checking the trustdb
    gpg: 3 marginal(s) needed, 1 complete(s) needed, PGP trust model
    gpg: depth: 0  valid:   1  signed:   0  trust: 0-, 0q, 0n, 0m, 0f, 1u
    pub   2048R/58614CEE 2014-03-25
          Key fingerprint = 1B9E 0C6D 71ED 827B 3327  8012 E688 95DE 5861 4CEE
    uid                  matcher <matcher@example.org>
    sub   2048R/64B4DEA4 2014-03-25

The new key pair is now created.

## Step 5 List key pairs

Verify that the key pairs are generated as expected:

    > gpg --homedir "$(pwd)" -k
    /brit-example/matcher/gpg/pubring.gpg
    ----------------------------------------------------------------------------------------
    pub   2048R/58614CEE 2014-03-25
    uid                  matcher <matcher@example.org>
    sub   2048R/64B4DEA4 2014-03-25

## Step 6 Export the Matcher public key to a file for use later by the Payers

Make a note of the public key identifier for the key you just generated.
In the key above it is "58614CEE"

Export your Matcher encryption public key from your keyring using:

    > gpg --homedir "$(pwd)" --armor --export 58614CEE > matcher-key.asc
    (change the "58614CEE" to your key identifier).

You can check the contents of the output file (making no changes) using:

    > gpg --dry-run --homedir "$(pwd)" --import matcher-key.asc

Once you are happy with the Matcher public key, create a directory `export-to-payer` as follows
and copy the `matcher-key.asc` to it.

Directory structure:

    matcher
      import-from-redeemer
      export-to-payer        << Copy the matcher-key.asc to here
      gpg

## Step 7 Tidy up

Delete the `S.gpg-agent` file in the `gpg` directory as it causes problems in Eclipse.
You will see a message:

    gpg-agent[4552]: can't connect my own socket: IPC connect call failed
    gpg-agent[4552]: this process is useless - shutting down
    gpg-agent[4552]: gpg-agent (GnuPG/MacGPG2) 2.0.22 stopped

This is OK, you can ignore it.

## Step 8 Start the Matcher daemon

Merge all the redeemer files into an 'all.txt'.

Start the Matcher daemon which:

* loads all the Redeemer Bitcoin addresses
* processes incoming Payer requests

See the brit-server readme.md for full instructions on setting up the brit-server on linux

# Summary

By performing the tasks in this document you have:

* constructed a PGP keypair that will be used by the Payers to encrypt their messages to the Matcher.
* exported the PGP keypair above for copying to the Payers.
* started the Matcher daemon service so that it can accept incoming Payer messages.
