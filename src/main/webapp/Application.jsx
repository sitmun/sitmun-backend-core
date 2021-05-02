import {Table} from "react-bootstrap";
import React from "react";

export function Application({application}) {
  return (
    <Table>
      <thead>
      <tr>
        <th>Property</th>
        <th>Value</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td><code>id</code></td>
        <td><code>{application.id}</code></td>
      </tr>
      <tr>
        <td><code>name</code></td>
        <td><code>{application.name}</code></td>
      </tr>
      <tr>
        <td><code>type</code></td>
        <td><code>{application.type}</code></td>
      </tr>
      <tr>
        <td><code>title</code></td>
        <td><code>{application.title}</code></td>
      </tr>
      <tr>
        <td><code>scales</code></td>
        <td><code>{application.scales?.join(', ')}</code></td>
      </tr>
      <tr>
        <td><code>srs</code></td>
        <td><code>{application.srs}</code></td>
      </tr>
      </tbody>
    </Table>
  );

}