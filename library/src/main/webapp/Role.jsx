import {Table} from "react-bootstrap";
import React from "react";

export function Role({role}) {
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
        <td><code>{role.id}</code></td>
      </tr>
      <tr>
        <td><code>name</code></td>
        <td><code>{role.name}</code></td>
      </tr>
      </tbody>
    </Table>
  );

}