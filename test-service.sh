#!/usr/bin/env bash

#NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
NEW_UUID="ASDJKL3789034ajsdasdjk9384asjdlk"
TEST_PATH="https://nimble-platform.uk-south.containers.mybluemix.net/object-store/${NEW_UUID}.txt"

verify_success() {
    if [[ $? != 0 ]] ; then
        echo "Failed on the last command - exiting!"
        exit 1
    fi
}

echo "Creating new temp file with data - ${NEW_UUID} , test path - ${TEST_PATH}"
echo ${NEW_UUID} > ${NEW_UUID}.txt

echo "Verifying file is deleted"
curl -X GET -I ${TEST_PATH} 2> /dev/null | head -n 1|cut -d$' ' -f2 | grep "404" > /dev/null
[ $? -eq 0 ] || (echo "ERROR curl Response wasn't 404"  && exit 1)

echo "Sending the file to object store service"
curl -X POST -d @${NEW_UUID}.txt ${TEST_PATH} 2> /dev/null && verify_success


echo "Retrieving the file from object store service"
curl -X GET -o testFile.txt ${TEST_PATH} 2> /dev/null && verify_success

[ -f testFile.txt ] || (echo "ERROR !!! File not found !" && exit 1)
echo "SUCCESS - downloaded successfully"

echo "Verifying the content of the file"
cat testFile.txt | grep ${NEW_UUID} > /dev/null
[ $? -eq 0 ] || (echo "The content of the file doesn't match"  && exit 1)


echo "Deleting file from object store"
curl -X DELETE ${TEST_PATH} 2> /dev/null && verify_success

sleep 1

echo "Verifying file is deleted"
curl -X GET -I ${TEST_PATH} 2> /dev/null | head -n 1|cut -d$' ' -f2 | grep "404" > /dev/null
[ $? -eq 0 ] || (echo "ERROR curl Response wasn't 404"  && exit 1)

echo "Deleting files 'tesFile.txt' & '${NEW_UUID}.txt' locally"
rm -f testFile.txt  ${NEW_UUID}.txt

echo "SUCCESS"
