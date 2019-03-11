#!/usr/bin/python

import argparse
import os
import sys
import pexpect
import getpass
import time

__author__ = 'eelimei'
__author__ = 'eqinann'

SSH_NEWKEY = '.*re you sure you want to continue.*'
COMMAND_PROMPT = '.*:~[\$,#]'
AUTHORIZED_KEYS_PATH = '/root/.ssh/authorized_keys'


def get_key_from_file_system():
    public_key = ''
    username = getpass.getuser()
    rsa_key_path = "/home/%s/.ssh/id_rsa.pub" % username
    print("Using KEY file: " + rsa_key_path)
    if os.path.isfile(rsa_key_path):
        with open(rsa_key_path, "r") as key_file:
            public_key = key_file.read().replace('\n', '')
    else:
        print("You don't have id_rsa.pub file in you .ssh folder,"
              " generate it first or provide the key as an argument.")
        sys.exit()
    return public_key


def upload_key(node, public_key):
    child = connect_to_lxc(node)
    child = connect_to_fuel_master(child)
    cic_list = get_controllers_ip(child)
    for cic in cic_list:
        child = connect_to_cic(child, cic)
        child = add_key_and_exit(child, public_key)
    child.kill(0)
    print("Keys are uploaded")


def connect_to_lxc(node):
    user = "lxcpxe"
    pwd = "Jumpstart"
    host = node + "lxc.cloud.k2.ericsson.se"
    prompt = (".*%s:~\$" % node)
    return create_connection(host, user, pwd, prompt)


def create_connection(host, user, pwd, prompt):
    connection = "ssh %s@%s" % (user, host)
    print("Connecting to %s" % connection)
    child = pexpect.spawn(connection, timeout=20)
    #child.logfile = sys.stdout  #enable debug
    return verify_ssh(child, host, pwd, prompt)


def connect_to_fuel_master(child):
    user = "root"
    pwd = "r00tme"
    hosts = ["192.168.0.11", "192.168.0.2", "192.168.2.11"]
    print('Detecting where Fuel master is')
    for iterator in hosts:
        if(host_is_pingable(child, iterator)):
            host = iterator
            break
    else:
        print("ERROR: Fuel not found.")
        sys.exit(1)
    print("Fuel master is detected on %s" % host)
    prompt = ".*\[root@.* ~\]#"
    print("Connecting to Fuel master node.")
    return connect_to(child, host, user, pwd, prompt)


def host_is_pingable(child, host):
    child.sendline('ping -c 1 %s' % host)
    time.sleep(1)
    result = child.expect(
        ['.*time=.*', '.*timeout.*', '.*Unreachable.*',
         '.*Packet filtered.*', '.*100% packet loss.*'])
    return result == 0


def connect_to_cic(child, cic):
    user = "root"
    pwd = ""
    prompt = ".*:~#"
    print("Connecting to cic: " + cic)
    return connect_to(child, cic, user, pwd, prompt)


def connect_to(child, host, user, pwd, prompt):
    child.sendline('ssh %s@%s' % (user, host))
    time.sleep(2) #in case connection takes time
    return verify_ssh(child, host, pwd, prompt)


def get_controllers_ip(child):
    print('Getting controllers\' IPs')
    cic_list = []
    child.sendline('fuel node | grep controller')
    fuel_prompt = ".*\[root@.* ~\]#"
    i = child.expect([pexpect.TIMEOUT, fuel_prompt, '.*192.*'])
    if i == 0:
        print('ERROR: Cannot find any controllers')
        sys.exit(1)
    if i == 1:
        print('ERROR: Cannot find any controllers')
        sys.exit(1)
    result = child.after
    index_cic_1 = result.find('192.168.0.')
    end_cic_1 = result.find(' ', index_cic_1 + 1)
    cic1 = result[index_cic_1: end_cic_1]
    cic_list.append(cic1)
    index_cic_2 = result.find('192.168.0.', index_cic_1 + 1)
    end_cic_2 = result.find(' ', index_cic_2 + 1)
    cic2 = result[index_cic_2: end_cic_2]
    cic_list.append(cic2)
    index_cic_3 = result.find('192.168.0.', index_cic_2 + 1)
    end_cic_3 = result.find(' ', index_cic_3 + 1)
    cic3 = result[index_cic_3: end_cic_3]
    cic_list.append(cic3)
    print('CICs found on Fuel master are: %s' % cic_list)
    return cic_list


def add_key_and_exit(child, public_key):
    if not key_exist(child, public_key):
        print("Adding key to authorized_keys file")
        child.sendline("echo %s >> %s" % (public_key, AUTHORIZED_KEYS_PATH))
    else:
        print("Key already exist, skipped...")
    child.sendline("exit")
    return child


def key_exist(child, public_key):
    child.sendline("grep \"%s\" /root/.ssh/authorized_keys | wc -l" %
                   public_key)
    # Here only handle 0 or 1 or 2 or 3 times of occurence
    key_exists = child.expect(['\r\n0', '\r\n1', '\r\n2', '\r\n3'])
    if key_exists:
        return True
    return False


def verify_ssh(child, host, pwd, prompt):
    i = child.expect(
        [pexpect.TIMEOUT, SSH_NEWKEY, '.*Connection refused.*',
         '.*No route to host.*', '.*Name or service not known.*',
         '.*ermission denied.*', '.*ost key verification failed.*',
         '.*authentication failures.*', '.*(?i)password:.*',
         prompt, COMMAND_PROMPT], timeout=20)
    if i == 0:  # Timeout
        print('Connection timed out. Host:%s Last words:%s' %
              (host, child.before))
        child.kill(0)
        sys.exit(1)
    if i == 1:  # In this case SSH does not have the public key cached.
        print('Fist time connecting to %s' % host)
        child.sendline('yes')
        verify_ssh(child, host, pwd, prompt)
    if i == 2: #Connection refused
        print('Connection refused on host: %s. Please check the host'
              ' address and try again' % host)
        sys.exit(1)
    if i == 3: #No route to host
        print 'No route to host: %s. Please check connection.' % host
        sys.exit(1)
    if i == 4: #Name or service not known
        print 'Name or service not known: %s. Please check DNS' % host
        sys.exit(1)
    if i == 5: #Permission denied
        print('Permission denied on host: %s. Please check authentication.' %
              host)
        sys.exit(1)
    if i == 6: #Host key verification failed
        print 'Host key verification failed for destination %s' % host
        print 'Remove the old KEY and try again'
        sys.exit(1)
    if i == 7: #too many authentication failures for user
        print 'Authentication failure. Host: %s' % host
        sys.exit(1)
    if i == 8: #password
        child.sendline(pwd)
        verify_ssh(child, host, pwd, prompt)
    if i == 9: #prompt
        pass
    if i == 10: #pre-defined prompt
        # This may happen if a public key was setup to automatically login.
        pass
    return (child)


def is_key_valid(key):
    ssh = key.find("ssh-rsa")
    if ssh == 0:
        return True
    else:
        return False


if __name__ == "__main__":
    public_key = ""
    node = ""
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", '--node', help="Name of this node. E.g. dc001",
                        type=str, required=True)
    parser.add_argument("-k", '--key', help="The content of user public key",
                        type=str)
    args = parser.parse_args()
    node = args.node.lower()
    if args.key:
        print("Public key is provided as a parameter")
        if is_key_valid(args.key):
            public_key = args.key
        else:
            print("Key provided is not valid. Refer to id_rsa.pub file.")
            sys.exit()
    else:
        print("Getting user public key content from file system")
        public_key = get_key_from_file_system()
        if not is_key_valid(public_key):
            print("Key in id_rsa.pub is not valid. Please generate again.")
            sys.exit()
    print("Uploading public key to " + node)
    upload_key(node, public_key)
