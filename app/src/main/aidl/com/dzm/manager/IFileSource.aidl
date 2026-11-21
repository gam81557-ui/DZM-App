package com.dzm.manager;

interface IFileSource {
    byte[] read(int maxBytes);
}
